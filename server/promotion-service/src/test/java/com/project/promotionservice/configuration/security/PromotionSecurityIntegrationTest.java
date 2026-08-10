package com.project.promotionservice.configuration.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionSecurityIntegrationTest {

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Autowired private MockMvc mockMvc;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void operationsRoleCanReachItsAdminReservationApi() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + token("OPERATIONS_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unrelatedAuthenticatedRoleIsForbiddenWithSerializableError() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + token("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unauthenticatedAdminRequestReturnsSerializableUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/admin/reservations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void signedTokenWithNonAccessTypeIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + token("ADMIN", "refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void everyAdminHandlerHasMethodOrClassAuthorizationPolicy() {
        List<String> unprotected = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getPatternValues().stream()
                        .anyMatch(path -> path.startsWith("/api/admin/")))
                .filter(entry -> !hasAuthorizationPolicy(entry.getValue()))
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .sorted()
                .toList();

        assertThat(unprotected)
                .as("Every /api/admin handler must remain protected when global routing is authenticated()")
                .isEmpty();
    }

    private boolean hasAuthorizationPolicy(HandlerMethod handler) {
        return AnnotatedElementUtils.findMergedAnnotation(
                handler.getMethod(), PreAuthorize.class) != null
                || AnnotatedElementUtils.findMergedAnnotation(
                handler.getBeanType(), PreAuthorize.class) != null;
    }

    private String token(String role) {
        return token(role, "access");
    }

    private String token(String role, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Date now = new Date();
        return Jwts.builder()
                .subject("security-test@lorafilm.vn")
                .claim("userId", 123L)
                .claim("role", role)
                .claim("permissions", List.of())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }
}
