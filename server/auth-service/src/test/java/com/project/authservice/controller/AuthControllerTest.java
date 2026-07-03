package com.project.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegistrationInitiatedResponse;
import com.project.authservice.service.AuthService;
import com.project.authservice.service.VerificationService;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private VerificationService verificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterSuccessReturns200() throws Exception {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "test@example.com", "0901234567", "092205006789", "2005-06-12", "Password@123");
        RegistrationInitiatedResponse mockResponse = new RegistrationInitiatedResponse("a1b2c3d4-e5f6-7890-abcd-ef1234567890", "Registration successful, please check your email for OTP");
        
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration initiated"))
                .andExpect(jsonPath("$.data.requestId").exists())
                // Ensure sensitive data is not leaked
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.cccd").doesNotExist())
                .andExpect(jsonPath("$.data.cccd").doesNotExist());
    }

    @Test
    void testLoginSuccessHidesToken() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "Password@123");
        // Using accessToken, ensuring no 'token' property
        JwtResponse mockResponse = new JwtResponse("access-token-123", "refresh-token-123", 3600L, "test@example.com", "CUSTOMER", 1L);
        
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
}
