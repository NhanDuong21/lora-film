package com.project.scoreservice.security;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 
@Configuration
@EnableWebSecurity
public class SecurityConfig {
 
    private final JwtFilter jwtFilter;
    private final InternalTokenFilter internalTokenFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
 
    public SecurityConfig(JwtFilter jwtFilter,
                          InternalTokenFilter internalTokenFilter,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.internalTokenFilter = internalTokenFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }
 
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/membership-tiers").permitAll()
                .requestMatchers("/internal/**").permitAll() // Internal security is verified by InternalTokenFilter
                // [TEMP - TO BE REMOVED WHEN RBAC IS IMPLEMENTED]: Temporary basic role check because fine-grained RBAC permissions are not yet implemented
                .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_SUPER_ADMIN", "SUPER_ADMIN")
                // [TODO - RE-ENABLE WHEN RBAC IS READY]: Fine-grained endpoint permissions for Score Service
                // .requestMatchers(HttpMethod.GET, "/api/admin/scores/users/{userId}").hasAuthority("SCORE_READ")
                // .requestMatchers(HttpMethod.GET, "/api/admin/scores/users/{userId}/history").hasAuthority("SCORE_READ")
                // .requestMatchers(HttpMethod.POST, "/api/admin/scores/users/{userId}/adjustments").hasAuthority("SCORE_ADJUST")
                // .requestMatchers(HttpMethod.POST, "/api/admin/scores/users/{userId}/recalculate-tier").hasAuthority("SCORE_MANAGE")
                // .requestMatchers(HttpMethod.GET, "/api/admin/membership-tiers", "/api/admin/membership-tiers/{tierId}").hasAuthority("MEMBERSHIP_TIER_READ")
                // .requestMatchers(HttpMethod.POST, "/api/admin/membership-tiers").hasAuthority("MEMBERSHIP_TIER_MANAGE")
                // .requestMatchers(HttpMethod.PUT, "/api/admin/membership-tiers/{tierId}").hasAuthority("MEMBERSHIP_TIER_MANAGE")
                // .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SCORE_MANAGE", "ROLE_SCORE_ADJUST", "ROLE_MEMBERSHIP_TIER_MANAGE")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(internalTokenFilter, JwtFilter.class);
 
        return http.build();
    }
}
