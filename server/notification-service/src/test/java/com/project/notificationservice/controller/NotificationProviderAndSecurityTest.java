package com.project.notificationservice.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotificationProviderAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private String generateToken(String role) {
        String secret = "dGVzdF9vbmx5X3NlY3JldF9rZXlfdGhhdF9pc19hdF9sZWFzdF8zMl9ieXRlc19sb25n";
        return "Bearer " + Jwts.builder()
                .subject("test@test.com")
                .claim("userId", 1L)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    private String generateExpiredToken(String role) {
        String secret = "dGVzdF9vbmx5X3NlY3JldF9rZXlfdGhhdF9pc19hdF9sZWFzdF8zMl9ieXRlc19sb25n";
        return "Bearer " + Jwts.builder()
                .subject("test@test.com")
                .claim("userId", 1L)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis() - 86400000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    private String generateInvalidSignatureToken(String role) {
        String wrongSecret = "d3Jvbmdfc2VjcmV0X2tleV9mb3JfdGVzdGluZ19zaWduYXR1cmVfdmFsaWRhdGlvbg==";
        return "Bearer " + Jwts.builder()
                .subject("test@test.com")
                .claim("userId", 1L)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(wrongSecret)))
                .compact();
    }

    @Test
    public void testSecurity_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    public void testSecurity_InvalidToken_Returns401() throws Exception {
        String invalidToken = generateInvalidSignatureToken("ADMIN");
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    public void testSecurity_ExpiredToken_Returns401() throws Exception {
        String expiredToken = generateExpiredToken("ADMIN");
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    public void testSecurity_Customer_Returns403() throws Exception {
        String customerToken = generateToken("CUSTOMER");
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN")))
                .andExpect(jsonPath("$.message", is("Forbidden")));
    }

    @Test
    public void testSecurity_Admin_Returns200() throws Exception {
        String adminToken = generateToken("ADMIN");
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSecurity_UnknownEndpoint_Returns404() throws Exception {
        String customerToken = generateToken("CUSTOMER");
        mockMvc.perform(get("/api/other")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Resource not found")));
    }
}
