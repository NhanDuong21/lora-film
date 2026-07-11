package com.lorafilm.movie.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCustomerAccessDenied() throws Exception {
        // Customer (without ROLE_ADMIN) accessing /api/admin/test
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized()); // Or Forbidden if authenticated as customer
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CUSTOMER"})
    public void testCustomerAccessDeniedWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    public void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isOk());
    }
}

@RestController
class TestAdminController {
    @GetMapping("/api/admin/test")
    public String adminTest() {
        return "Admin Area";
    }
}
