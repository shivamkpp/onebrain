package dev.onebrain.identity;

import dev.onebrain.authorization.ScopeAccess;
import java.security.Principal;
import java.util.List;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    private final ScopeAccess access;

    public SessionController(ScopeAccess access) { this.access = access; }

    public record Session(String username, List<ScopeAccess.Scope> scopes, String csrfToken, String csrfHeader) {}

    @GetMapping("/api/v1/session")
    Session session(Principal principal, CsrfToken token) {
        return new Session(principal.getName(), access.readableScopes(principal.getName()),
                token.getToken(), token.getHeaderName());
    }
}
