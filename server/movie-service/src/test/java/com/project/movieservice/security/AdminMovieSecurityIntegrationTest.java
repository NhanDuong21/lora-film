package com.project.movieservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminMovieSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void whenAnonymous_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/movies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void whenCustomer_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/movies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAdmin_thenOk() throws Exception {
        mockMvc.perform(get("/api/admin/movies"))
                .andExpect(status().isOk());
    }
}
