package com.lorafilm.movie.integration.tmdb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TmdbAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/tmdb/sync/state"))
                .andExpect(status().isUnauthorized());
                
        mockMvc.perform(post("/api/admin/tmdb/sync/bulk/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CUSTOMER"})
    public void testCustomerAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/tmdb/sync/state"))
                .andExpect(status().isForbidden());
                
        mockMvc.perform(post("/api/admin/tmdb/sync/bulk/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    public void testAdminAccessAllowed() throws Exception {
        // Since we are not mocking the actual service here, it might return 200 or 500 depending on DB state etc., 
        // but it will NOT return 401 or 403. We'll just verify it's not 401/403.
        mockMvc.perform(get("/api/admin/tmdb/sync/state"))
                .andExpect(status().isOk());
    }
}
