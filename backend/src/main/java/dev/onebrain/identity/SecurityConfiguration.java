package dev.onebrain.identity;

import dev.onebrain.api.ApiError;
import dev.onebrain.api.RequestIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfiguration {
    @Bean
    UserDetailsService localUsers(Environment environment) {
        var users = new InMemoryUserDetailsManager();
        // No default generated account and no usable identity adapter outside the local profile.
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            String password = environment.getRequiredProperty("onebrain.local-password");
            if (password.isBlank()) {
                throw new IllegalStateException("ONEBRAIN_LOCAL_PASSWORD must not be blank");
            }
            String encoded = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password);
            for (String name : new String[]{"contributor", "reader", "outsider", "external"}) {
                users.createUser(User.withUsername(name).password(encoded).roles("LOCAL").build());
            }
        }
        return users;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                .addFilterBefore(new RequestIdFilter(), SecurityContextHolderFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) ->
                        ApiError.write(response, 401, "UNAUTHENTICATED", "Sign in to continue.")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                ApiError.write(response, 401, "UNAUTHENTICATED", "Sign in to continue."))
                        .accessDeniedHandler((request, response, exception) ->
                                ApiError.write(response, 403, "FORBIDDEN", "This action is not permitted.")))
                // HTTP Basic is automatically sent by browsers: mutation requests still require CSRF.
                .csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }
}
