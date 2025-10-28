// src/main/java/org/wp2/medsys/SecurityConfig.java
package org.wp2.medsys;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // lets you use @PreAuthorize later
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Authorization
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/register",
                                "/error",               // avoid redirect loops
                                "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/logo.png"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Form login (custom page)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/portal", true)
                        .failureUrl("/login?error") // show error alert in login.html
                        .permitAll()
                )

                // Logout (POST with CSRF token; matches your base.html form)
                .logout(log -> log
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        // CSRF is enabled by default; templates include the token.
        // If you add H2 console later, uncomment:
        // http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        // http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // same bean as before
    }
}
