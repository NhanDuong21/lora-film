package com.project.userservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetProfile_MissingToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.errorCode").value("AUTH_UNAUTHORIZED"));
    }
    
    @Test
    public void testGetProfile_MalformedToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"))
                .andExpect(jsonPath("$.errorCode").value("AUTH_UNAUTHORIZED"));
    }
}
