package com.project.paymentservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(JwtFilter jwtFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                        "/v3/api-docs", "/health", "/", "/error").permitAll()
                .requestMatchers("/api/payments/callback/**", "/api/payments/return/**").permitAll()
                .requestMatchers("/internal/payments/refunds/**", "/internal/payments/emergency/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/payments/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_ACCOUNTANT", "PERM_VIEW_FINANCE",
                            "PAYMENT_VIEW", "PAYMENT_RECONCILE", "AUDIT_VIEW")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/settlements")
                    .hasAnyAuthority("ROLE_ADMIN", "SETTLEMENT_IMPORT")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/settlements/*/lock")
                    .hasAnyAuthority("ROLE_ADMIN", "SETTLEMENT_LOCK")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/cash-sessions/*/verify")
                    .hasAnyAuthority("ROLE_ADMIN", "CASH_CLOSE_VERIFY")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/refunds/*/requests")
                    .hasAnyAuthority("ROLE_ADMIN", "REFUND_REQUEST")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/refunds/*/approve",
                        "/api/admin/payments/accounting/refunds/*/reject")
                    .hasAnyAuthority("ROLE_ADMIN", "REFUND_APPROVE")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/periods")
                    .hasAnyAuthority("ROLE_ADMIN", "ACCOUNTING_PERIOD_CREATE")
                .requestMatchers(HttpMethod.POST, "/api/admin/payments/accounting/periods/*/actions")
                    .hasAnyAuthority("ROLE_ADMIN", "ACCOUNTING_PERIOD_RECONCILE", "ACCOUNTING_PERIOD_CLOSE")
                .requestMatchers("/api/admin/payments/reconciliations/*/assign",
                        "/api/admin/payments/reconciliations/*/resolve")
                    .hasAnyAuthority("ROLE_ADMIN", "PAYMENT_RECONCILE")
                .requestMatchers("/api/admin/payments/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/payments/**").hasRole("MANAGER")
                .requestMatchers("/api/employee/**")
                    .access(new WebExpressionAuthorizationManager(
                            "hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('PAYMENT_CASH_COLLECT'))"))
                .requestMatchers("/api/payments/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
