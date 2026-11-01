package dev.onebrain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class LocalIdentityTest {
    @Test
    void defaultProfileHasNoDevelopmentAccounts() {
        var users = new SecurityConfiguration().localUsers(new MockEnvironment());
        assertThatThrownBy(() -> users.loadUserByUsername("contributor"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void localProfileRequiresANonblankPassword() {
        var environment = new MockEnvironment().withProperty("onebrain.local-password", " ");
        environment.setActiveProfiles("local");
        assertThatThrownBy(() -> new SecurityConfiguration().localUsers(environment))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void localProfileUsesEncodedPasswords() {
        var environment = new MockEnvironment().withProperty("onebrain.local-password", "synthetic-test-password");
        environment.setActiveProfiles("local");
        var user = new SecurityConfiguration().localUsers(environment).loadUserByUsername("contributor");
        assertThat(user.getPassword()).startsWith("{bcrypt}").doesNotContain("synthetic-test-password");
    }
}
