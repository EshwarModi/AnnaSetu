package com.annasetu.config;

import com.annasetu.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Responsibility:
 * Central Spring Security 6 configuration establishing role-based URL protection,
 * form login/logout endpoints, CSRF protection, session fixation mitigation, and remember-me.
 *
 * Domain & Architecture Context:
 * - Secures /donor/** for DONOR, /org/** for ORGANIZATION, and /admin/** for ADMIN.
 * - Whitelists public discovery endpoints, registration forms, and static CSS/JS assets.
 * - Routes authenticated users dynamically to their persona-specific dashboards.
 *
 * Interview Talking Points:
 * 1. Role-Based Access Control (RBAC): Strict URL hierarchy matching (/donor/**, /org/**, /admin/**).
 * 2. Session Fixation Protection: Configured with {@code changeSessionId()} so an existing session identifier
 *    is discarded and replaced immediately upon successful authentication.
 * 3. CSRF & Form Security: CSRF is enforced across all state-changing endpoints, working automatically
 *    with Thymeleaf's {@code th:action}.
 * 4. Remember-Me Authentication: Provides persistent token support across browser restarts.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          CustomAuthenticationSuccessHandler authenticationSuccessHandler,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            // 1. Role-based URL Authorization
            .authorizeHttpRequests(auth -> auth
                // Publicly accessible pages & static resources
                .requestMatchers(
                    "/",
                    "/home",
                    "/search",
                    "/organizations/**",
                    "/api/**",
                    "/login",
                    "/register/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error",
                    "/403",
                    "/404"
                ).permitAll()
                // Role-restricted routes
                .requestMatchers("/donor/**").hasRole("DONOR")
                .requestMatchers("/org/**").hasRole("ORGANIZATION")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // 2. Custom Form Login Configuration
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // 3. Remember-Me Authentication
            .rememberMe(remember -> remember
                .key("AnnaSetuSecretRememberMeKey2026")
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                .userDetailsService(userDetailsService)
                .rememberMeParameter("remember-me")
            )
            // 4. Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            // 5. Session Fixation Mitigation
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
            )
            // 6. Exception Handling (Custom 403 Access Denied)
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
