package com.project.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.dto.request.ReferenceDto;
import com.project.notificationservice.dto.request.SendNotificationRequest;
import com.project.notificationservice.entity.NotificationLog;
import com.project.notificationservice.entity.NotificationTemplate;
import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.config.NotificationProviderProperties;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InternalNotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationLogRepository logRepository;

    @Autowired
    private NotificationProviderProperties providerProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String INTERNAL_TOKEN = "secret-internal-token";

    @BeforeEach
    public void setUp() {
        logRepository.deleteAll();
        templateRepository.deleteAll();
        providerProperties.getMockEmail().setSimulateFailure(false);
    }

    private SendNotificationRequest buildBaseRequest() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRequestSource("test-service");
        request.setUserId(1L);
        request.setEventId("EVT-TEST-123");
        request.setChannelType(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        return request;
    }

    @Test
    public void testSendEmail_Template_Success() throws Exception {
        NotificationTemplate template = new NotificationTemplate(
                "TICKET_CONFIRMATION",
                "Ticket confirmed for {name}",
                "Hello {name}, booking {bookingCode} is ready.",
                NotificationChannel.EMAIL,
                true
        );
        templateRepository.saveAndFlush(template);

        SendNotificationRequest request = buildBaseRequest();
        request.setTemplateCode("TICKET_CONFIRMATION");
        request.setVariables(Map.of("name", "Alice", "bookingCode", "BK-999"));

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.recipient", is("t***@example.com")))
                .andExpect(jsonPath("$.data.templateCode", is("TICKET_CONFIRMATION")));

        // Verify log was saved in DB
        NotificationLog saved = logRepository.findByEventId("EVT-TEST-123").orElse(null);
        assertNotNull(saved);
        assertEquals("SENT", saved.getStatus());
        assertEquals("test@example.com", saved.getRecipient());
        assertEquals("Ticket confirmed for Alice", saved.getActualTitle());
        assertEquals("Hello Alice, booking BK-999 is ready.", saved.getActualContent());
    }

    @Test
    public void testSendInApp_Template_Success() throws Exception {
        NotificationTemplate template = new NotificationTemplate(
                "INAPP_WELCOME",
                "Welcome!",
                "Welcome to our app, {name}.",
                NotificationChannel.IN_APP,
                true
        );
        templateRepository.saveAndFlush(template);

        SendNotificationRequest request = buildBaseRequest();
        request.setChannelType(NotificationChannel.IN_APP);
        request.setRecipient(null); // Recipient can remain null for IN_APP
        request.setTemplateCode("INAPP_WELCOME");
        request.setVariables(Map.of("name", "Bob"));

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.recipient", is(nullValue())));

        NotificationLog saved = logRepository.findByEventId("EVT-TEST-123").orElse(null);
        assertNotNull(saved);
        assertEquals("SENT", saved.getStatus());
        assertNull(saved.getRecipient()); // stored recipient remains null
        assertEquals("Welcome to our app, Bob.", saved.getActualContent());
    }

    @Test
    public void testSendEmail_FreeForm_Success() throws Exception {
        SendNotificationRequest request = buildBaseRequest();
        request.setTitle("Alert title");
        request.setContent("This is a free-form message");
        request.setReference(new ReferenceDto("BOOKING", "B1001"));

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.templateCode", is(nullValue())));

        NotificationLog saved = logRepository.findByEventId("EVT-TEST-123").orElse(null);
        assertNotNull(saved);
        assertEquals("SENT", saved.getStatus());
        assertEquals("Alert title", saved.getActualTitle());
        assertEquals("This is a free-form message", saved.getActualContent());
        assertEquals("BOOKING", saved.getReferenceType());
        assertEquals("B1001", saved.getReferenceId());
    }

    @Test
    public void testSendInApp_FreeForm_Success() throws Exception {
        SendNotificationRequest request = buildBaseRequest();
        request.setChannelType(NotificationChannel.IN_APP);
        request.setRecipient(null);
        request.setTitle("In-App alert");
        request.setContent("This is in-app freeform");

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SENT")));

        NotificationLog saved = logRepository.findByEventId("EVT-TEST-123").orElse(null);
        assertNotNull(saved);
        assertEquals("SENT", saved.getStatus());
        assertNull(saved.getRecipient());
    }

    @Test
    public void testSend_TemplateDisabled_Returns409() throws Exception {
        NotificationTemplate template = new NotificationTemplate(
                "DISABLED_TEMP",
                "Title",
                "Content {otp}",
                NotificationChannel.EMAIL,
                false // disabled
        );
        templateRepository.saveAndFlush(template);

        SendNotificationRequest request = buildBaseRequest();
        request.setTemplateCode("DISABLED_TEMP");
        request.setVariables(Map.of("otp", "1234"));

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_TEMPLATE_DISABLED")));

        // DB log should NOT be created for template validation errors
        assertTrue(logRepository.findAll().isEmpty());
    }

    @Test
    public void testSend_MissingVariable_Returns400WithDetails() throws Exception {
        NotificationTemplate template = new NotificationTemplate(
                "OTP_TEMP",
                "OTP for {name}",
                "Your OTP is {otp}",
                NotificationChannel.EMAIL,
                true
        );
        templateRepository.saveAndFlush(template);

        SendNotificationRequest request = buildBaseRequest();
        request.setTemplateCode("OTP_TEMP");
        request.setVariables(Map.of("name", "Alice")); // missing "otp"

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_TEMPLATE_VARIABLE_MISSING")))
                .andExpect(jsonPath("$.data.missingVariables", hasItem("otp")));

        // DB log should NOT be created for rendering validation errors
        assertTrue(logRepository.findAll().isEmpty());
    }

    @Test
    public void testSend_ChannelMismatch_Returns400WithDetails() throws Exception {
        NotificationTemplate template = new NotificationTemplate(
                "EMAIL_TEMP",
                "Title",
                "Content",
                NotificationChannel.EMAIL,
                true
        );
        templateRepository.saveAndFlush(template);

        SendNotificationRequest request = buildBaseRequest();
        request.setTemplateCode("EMAIL_TEMP");
        request.setChannelType(NotificationChannel.IN_APP); // channel mismatch: request says IN_APP, template is EMAIL

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_TEMPLATE_CHANNEL_MISMATCH")))
                .andExpect(jsonPath("$.data.templateChannel", is("EMAIL")))
                .andExpect(jsonPath("$.data.requestedChannel", is("IN_APP")));
    }

    @Test
    public void testSend_ProviderFailure_Returns502() throws Exception {
        // Configure provider mock to simulate failure
        providerProperties.getMockEmail().setSimulateFailure(true);
        providerProperties.getMockEmail().setFailureCode("PROVIDER_TIMEOUT");
        providerProperties.getMockEmail().setRetryable(true);

        SendNotificationRequest request = buildBaseRequest();
        request.setTitle("Freeform title");
        request.setContent("Content");

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_SEND_FAILED")));

        // DB log must persist as FAILED even if exception was thrown to the caller
        NotificationLog saved = logRepository.findByEventId("EVT-TEST-123").orElse(null);
        assertNotNull(saved);
        assertEquals("FAILED", saved.getStatus());
        assertEquals("PROVIDER_TIMEOUT", saved.getFailureCode());
        assertEquals("Simulated mock failure", saved.getErrorMessage());
    }

    @Test
    public void testSend_DuplicateRequest_Returns200WithIdempotentResponse() throws Exception {
        SendNotificationRequest request = buildBaseRequest();
        request.setTitle("Idempotent title");
        request.setContent("Content");

        // Send first request (201 Created)
        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.idempotent", is(nullValue())));

        // Send duplicate request (200 OK)
        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.idempotent", is(true)));
    }

    @Test
    public void testSend_InvalidToken_Returns401() throws Exception {
        SendNotificationRequest request = buildBaseRequest();
        request.setTitle("Title");
        request.setContent("Content");

        mockMvc.perform(post("/internal/notifications/send")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }
}
