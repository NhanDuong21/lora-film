package com.project.userservice.controller;

import com.project.userservice.entity.User;
import com.project.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalUserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setAccountId(42L);
        user.setFullName("Nguyen Van A");
        user.setEmail("customer@example.com");
        user.setAvatarUrl("/uploads/avatar-42.png");
        userRepository.save(user);
    }

    @Test
    void rejectsMissingInternalToken() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/42/notification-recipient"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_TOKEN_INVALID"));
    }

    @Test
    void returnsMinimalRecipientForValidInternalToken() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/42/notification-recipient")
                        .header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(42))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"));
    }

    @Test
    void returnsDisplayIdentityForCashSessionDirectory() throws Exception {
        mockMvc.perform(post("/api/v1/internal/employees/directory")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountIds\":[42]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountId").value(42))
                .andExpect(jsonPath("$.data[0].fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data[0].avatarUrl").value("/uploads/avatar-42.png"));
    }
}
