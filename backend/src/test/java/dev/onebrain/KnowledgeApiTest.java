package dev.onebrain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.onebrain.knowledge.CreateNote;
import dev.onebrain.knowledge.KnowledgeService;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class KnowledgeApiTest {
    static final String PLATFORM = "30000000-0000-0000-0000-000000000001";
    static final String PAYMENTS = "30000000-0000-0000-0000-000000000002";
    static final String EXTERNAL = "30000000-0000-0000-0000-000000000003";
    static final JsonMapper JSON = new JsonMapper();

    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(DockerImageName.parse(
            "postgres@sha256:3a4264520e121176619f54ff867d12774bc81e95e7b84db58d63bccdb6144b81"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", DATABASE::getJdbcUrl);
        properties.add("spring.datasource.username", DATABASE::getUsername);
        properties.add("spring.datasource.password", DATABASE::getPassword);
        properties.add("onebrain.local-password", () -> "synthetic-test-password");
    }

    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @Autowired KnowledgeService service;

    @Test
    void createsAndReadsPersistedNoteWithProvenanceRevisionAndAudit() throws Exception {
        Account contributor = signIn("contributor");
        Instant before = Instant.now();
        var response = contributor.post(note(PLATFORM), true);
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode item = JSON.readTree(response.body());
        String id = item.get("id").asText();
        assertThat(response.headers().firstValue("Location")).contains("/api/v1/knowledge/" + id);
        assertThat(item.get("createdBy").asText()).isEqualTo("contributor");
        assertThat(item.get("revision").asInt()).isEqualTo(1);
        assertThat(item.get("lifecycleStatus").asText()).isEqualTo("DRAFT");
        assertThat(item.get("itemType").asText()).isEqualTo("NOTE");
        assertThat(item.get("ownerTeamId").asText()).isEqualTo("20000000-0000-0000-0000-000000000001");
        assertThat(Instant.parse(item.get("createdAt").asText())).isAfterOrEqualTo(before);
        assertThat(item.get("updatedAt")).isEqualTo(item.get("createdAt"));
        var read = signIn("reader").get("/knowledge/" + id);
        assertThat(read.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(read.body())).isEqualTo(item);
        assertThat(item.get("source").get("url").asText()).isEqualTo("https://example.com/synthetic/adr-1");
        assertThat(jdbc.sql("SELECT count(*) FROM knowledge_revision WHERE knowledge_item_id = :id")
                .param("id", UUID.fromString(id)).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT request_id::text FROM audit_event WHERE target_id = :id")
                .param("id", UUID.fromString(id)).query(String.class).single())
                .isEqualTo(response.headers().firstValue("X-Request-Id").orElseThrow());
        assertThat(jdbc.sql("SELECT content_sha256 FROM knowledge_revision WHERE knowledge_item_id = :id")
                .param("id", UUID.fromString(id)).query(String.class).single()).matches("[0-9a-f]{64}");
    }

    @Test
    void manualNoteMayOmitSupportingSource() throws Exception {
        var response = signIn("contributor").post(JSON.writeValueAsString(Map.of(
                "title", "Synthetic note", "body", "Manual knowledge", "visibilityScopeId", PLATFORM)), true);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JSON.readTree(response.body()).get("source").isNull()).isTrue();
    }

    @Test
    void missingAndInaccessibleNotesHaveTheSameSafeResponse() throws Exception {
        String id = createdId();
        for (String name : new String[]{"outsider", "external"}) {
            Account account = signIn(name);
            var denied = account.get("/knowledge/" + id);
            var missing = account.get("/knowledge/" + UUID.randomUUID());
            assertThat(denied.statusCode()).isEqualTo(404);
            assertThat(missing.statusCode()).isEqualTo(404);
            assertThat(JSON.readTree(denied.body()).get("code").asText()).isEqualTo("NOT_FOUND");
            assertThat(JSON.readTree(denied.body()).get("message"))
                    .isEqualTo(JSON.readTree(missing.body()).get("message"));
            assertThat(denied.body()).doesNotContain("Synthetic decision", "adr-1", "contributor");
        }
    }

    @Test
    void readersCannotWriteAndContributorsCannotChooseAnotherScope() throws Exception {
        int before = countItems();
        assertThat(signIn("reader").post(note(PLATFORM), true).statusCode()).isEqualTo(403);
        for (String scope : new String[]{PAYMENTS, EXTERNAL, UUID.randomUUID().toString()}) {
            assertThat(signIn("contributor").post(note(scope), true).statusCode()).isEqualTo(403);
        }
        assertThat(countItems()).isEqualTo(before);
    }

    @Test
    void revocationTakesEffectOnTheNextRead() throws Exception {
        String id = createdId();
        Account reader = signIn("reader");
        assertThat(reader.get("/knowledge/" + id).statusCode()).isEqualTo(200);
        jdbc.sql("DELETE FROM resource_grant WHERE principal_name = 'reader' AND scope_id = :scope")
                .param("scope", UUID.fromString(PLATFORM)).update();
        try {
            assertThat(reader.get("/knowledge/" + id).statusCode()).isEqualTo(404);
        } finally {
            jdbc.sql("""
                    INSERT INTO resource_grant VALUES
                    ('10000000-0000-0000-0000-000000000001', 'reader', :scope, 'READ')
                    """).param("scope", UUID.fromString(PLATFORM)).update();
        }
    }

    @Test
    void sessionExposesOnlyGrantedScopesAndCapabilities() throws Exception {
        JsonNode reader = signIn("reader").session;
        assertThat(reader.get("scopes").size()).isEqualTo(1);
        assertThat(reader.get("scopes").get(0).get("id").asText()).isEqualTo(PLATFORM);
        assertThat(reader.get("scopes").get(0).get("canContribute").asBoolean()).isFalse();
        assertThat(signIn("external").session.get("scopes").get(0).get("id").asText()).isEqualTo(EXTERNAL);
    }

    @Test
    void csrfAndAuthenticationAreRequired() throws Exception {
        int before = countItems();
        assertThat(signIn("contributor").post(note(PLATFORM), false).statusCode()).isEqualTo(403);
        var unauthenticated = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/session")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
        var wrongPassword = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/session"))
                .header("Authorization", basic("contributor", "incorrect-password")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(countItems()).isEqualTo(before);
    }

    @Test
    void malformedIdAndUnknownRouteKeepTheirClientErrorStatus() throws Exception {
        Account contributor = signIn("contributor");
        assertThat(contributor.get("/knowledge/invalid-id").statusCode()).isEqualTo(400);
        var missingRoute = contributor.get("/not-a-route");
        assertThat(missingRoute.statusCode()).as(missingRoute.body()).isEqualTo(404);
        assertThat(JSON.readTree(missingRoute.body()).get("code").asText()).isEqualTo("NOT_FOUND");
    }

    @ParameterizedTest
    @MethodSource("invalidNotes")
    void rejectsInvalidOrSpoofedInputWithoutWriting(String input) throws Exception {
        int before = countItems();
        var response = signIn("contributor").post(input, true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(JSON.readTree(response.body()).get("code").asText()).isEqualTo("INVALID_REQUEST");
        assertThat(response.body()).doesNotContain("SQLException", "stackTrace", "javascript:");
        assertThat(countItems()).isEqualTo(before);
    }

    static Stream<String> invalidNotes() {
        return Stream.of(
                note(PLATFORM).replace("Synthetic decision", " "),
                note(PLATFORM).replace("Synthetic decision", "a".repeat(201)),
                note(PLATFORM).replace("Synthetic body", "a".repeat(20001)),
                note(PLATFORM).replace("Synthetic body", ""),
                note(PLATFORM).replace("https://example.com/synthetic/adr-1", "javascript:alert(1)"),
                note(PLATFORM).replace("https://example.com/synthetic/adr-1", "https://name:secret@example.com"),
                note(PLATFORM).replace("https://example.com/synthetic/adr-1", "https://"),
                note(PLATFORM).replace("\"visibilityScopeId\"", "\"unknownScope\""),
                note(PLATFORM).replaceFirst("\\{", "{\"createdBy\":\"admin\","),
                note(PLATFORM).replaceFirst("\\{", "{\"organizationId\":\"" + EXTERNAL + "\","),
                "{malformed json}");
    }

    @Test
    void failedAuditRollsBackBothItemAndRevision() {
        int itemsBefore = countItems();
        int revisionsBefore = jdbc.sql("SELECT count(*) FROM knowledge_revision").query(Integer.class).single();
        assertThatThrownBy(() -> service.create("contributor",
                new CreateNote("Atomic synthetic note", "Body", UUID.fromString(PLATFORM), null), null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(countItems()).isEqualTo(itemsBefore);
        assertThat(jdbc.sql("SELECT count(*) FROM knowledge_revision").query(Integer.class).single())
                .isEqualTo(revisionsBefore);
    }

    @Test
    void databaseRejectsCrossOrganizationGrants() {
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO resource_grant VALUES
                ('10000000-0000-0000-0000-000000000002', 'external', :scope, 'CONTRIBUTE')
                """).param("scope", UUID.fromString(PLATFORM)).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int countItems() {
        return jdbc.sql("SELECT count(*) FROM knowledge_item").query(Integer.class).single();
    }

    private String createdId() throws Exception {
        var response = signIn("contributor").post(note(PLATFORM), true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asText();
    }

    private Account signIn(String name) throws Exception {
        Account account = new Account(name);
        var response = account.get("/session");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        account.session = JSON.readTree(response.body());
        return account;
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + "/api/v1" + path); }

    private static String note(String scope) {
        return """
                {"title":"Synthetic decision","body":"Synthetic body","visibilityScopeId":"%s",
                 "source":{"title":"Synthetic ADR","url":"https://example.com/synthetic/adr-1"}}
                """.formatted(scope);
    }

    private static String basic(String name, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((name + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private class Account {
        final String name;
        final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        JsonNode session;

        Account(String name) { this.name = name; }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path))
                    .header("Authorization", basic(name, "synthetic-test-password")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String input, boolean csrf) throws Exception {
            var builder = HttpRequest.newBuilder(uri("/knowledge"))
                    .header("Authorization", basic(name, "synthetic-test-password"))
                    .header("Content-Type", "application/json");
            if (csrf) builder.header(session.get("csrfHeader").asText(), session.get("csrfToken").asText());
            return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(input)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
