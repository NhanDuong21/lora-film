package com.project.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class RouteValidator {

    public static final Set<String> OPEN_ENDPOINTS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify",
            "/api/auth/verify-email",
            "/api/auth/send-otp",
            "/api/auth/refresh-token",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/health"
    );
    private static final Pattern REGISTRATION_STATUS =
            Pattern.compile("^/api/auth/registrations/[^/]+/status$");
    private static final Set<String> PUBLIC_GET_PREFIXES = Set.of(
            "/api/customer/movies",
            "/api/customer/genres",
            "/api/customer/showtimes",
            "/api/customer/concessions",
            "/api/cinemas",
            "/api/showtimes",
            "/api/membership-tiers",
            // Public promotion catalog is displayed on the homepage and can
            // be browsed by guests. Claiming a voucher remains protected.
            "/api/promotions/public"
    );
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/payments/callback",
            "/api/payments/return"
    );

    public final Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                return request.getMethod() != HttpMethod.OPTIONS
                        && !OPEN_ENDPOINTS.contains(path)
                        // Seat availability is a public, read-only projection.
                        // Browser WebSocket handshakes cannot attach our Bearer
                        // header, so requiring gateway authentication here
                        // rejects Socket.IO upgrades with HTTP 401.
                        && !path.startsWith("/socket.io/")
                        && !(request.getMethod() == HttpMethod.GET && isPublicGetPath(path))
                        && !isPublicPath(path)
                        && !REGISTRATION_STATUS.matcher(path).matches()
                        && !path.startsWith("/oauth2/")
                        && !path.startsWith("/login/oauth2/")
                        && !path.startsWith("/actuator/health")
                        && !path.startsWith("/v3/api-docs")
                        && !path.startsWith("/swagger-ui")
                        && !path.startsWith("/api/users/profile/avatar/files/");
            };

    private static boolean isPublicGetPath(String path) {
        return PUBLIC_GET_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private static boolean isPublicPath(String path) {
        return PUBLIC_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
