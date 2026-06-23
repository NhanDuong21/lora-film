package com.project.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.dto.request.CreateNotificationTemplateRequest;
import com.project.notificationservice.dto.request.UpdateNotificationTemplateRequest;
import com.project.notificationservice.dto.request.UpdateNotificationTemplateStatusRequest;
import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.entity.NotificationTemplate;
import com.project.notificationservice.repository.NotificationTemplateRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NotificationTemplateAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationTemplateRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
        adminToken = "Bearer " + generateToken("ADMIN");
        userToken = "Bearer " + generateToken("USER");
    }

    private String generateToken(String role) {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        return Jwts.builder()
                .subject("test@test.com")
                .claim("userId", 1L)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    @Test
    public void testCreateTemplate_Success() throws Exception {
        CreateNotificationTemplateRequest request = new CreateNotificationTemplateRequest();
        request.setTemplateCode("  ticket_confirmation  "); // test trim and uppercase normalization
        request.setTitle("  Xác nhận vé LoraFilm  ");
        request.setContent("  Xin chào {name}, vé của bạn có mã {bookingCode}.  ");
        request.setChannelType(NotificationChannel.EMAIL);
        request.setIsActive(true);

        mockMvc.perform(post("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("created successfully")))
                .andExpect(jsonPath("$.data.templateCode", is("TICKET_CONFIRMATION")))
                .andExpect(jsonPath("$.data.title", is("Xác nhận vé LoraFilm")))
                .andExpect(jsonPath("$.data.content", is("Xin chào {name}, vé của bạn có mã {bookingCode}.")))
                .andExpect(jsonPath("$.data.channelType", is("EMAIL")))
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.version", is(0)));
    }

    @Test
    public void testCreateTemplate_DuplicateCode() throws Exception {
        NotificationTemplate existing = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        repository.save(existing);

        CreateNotificationTemplateRequest request = new CreateNotificationTemplateRequest();
        request.setTemplateCode("ticket_confirmation");
        request.setTitle("Title");
        request.setContent("Content");
        request.setChannelType(NotificationChannel.EMAIL);

        mockMvc.perform(post("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_TEMPLATE_CODE_ALREADY_EXISTS")));
    }

    @Test
    public void testCreateTemplate_ValidationFailed() throws Exception {
        CreateNotificationTemplateRequest request = new CreateNotificationTemplateRequest();
        request.setTemplateCode("INVALID-CODE-WITH-DASHES!"); // pattern validation fail
        request.setTitle(""); // blank fail
        request.setContent("Content");
        request.setChannelType(null); // null fail

        mockMvc.perform(post("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testCreateTemplate_InvalidChannelEnum() throws Exception {
        String invalidJsonPayload = "{\n" +
                "  \"templateCode\": \"TICKET_CONFIRMATION\",\n" +
                "  \"title\": \"Xác nhận vé LoraFilm\",\n" +
                "  \"content\": \"Hello\",\n" +
                "  \"channelType\": \"INVALID_CHANNEL\"\n" +
                "}";

        mockMvc.perform(post("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_INVALID_CHANNEL")));
    }

    @Test
    public void testGetTemplateList_SuccessAndFiltering() throws Exception {
        NotificationTemplate t1 = new NotificationTemplate("TICKET_CONFIRMATION", "Xác nhận", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate t2 = new NotificationTemplate("OTP_CODE", "Mã OTP", "Content", NotificationChannel.SMS, false);
        repository.save(t1);
        repository.save(t2);

        // Test filter by exact templateCode (case-insensitive)
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .param("code", "otp_code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].templateCode", is("OTP_CODE")));

        // Test filter by channel
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .param("channelType", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].templateCode", is("TICKET_CONFIRMATION")));

        // Test filter by isActive
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", adminToken)
                        .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].templateCode", is("OTP_CODE")));
    }

    @Test
    public void testGetTemplateDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/admin/notification-templates/99999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_TEMPLATE_NOT_FOUND")));
    }

    @Test
    public void testUpdateTemplate_Success() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.save(template);

        UpdateNotificationTemplateRequest request = new UpdateNotificationTemplateRequest();
        request.setTemplateCode("TICKET_CONFIRMATION");
        request.setTitle("Updated Title");
        request.setContent("Updated Content");
        request.setChannelType(NotificationChannel.EMAIL);
        request.setIsActive(true);
        request.setVersion(saved.getVersion());

        mockMvc.perform(put("/api/admin/notification-templates/" + saved.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Updated Title")))
                .andExpect(jsonPath("$.data.content", is("Updated Content")))
                .andExpect(jsonPath("$.data.version", is(saved.getVersion() + 1)));
    }

    @Test
    public void testUpdateTemplate_ImmutableCodeViolation() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.save(template);

        UpdateNotificationTemplateRequest request = new UpdateNotificationTemplateRequest();
        request.setTemplateCode("MODIFIED_CODE"); // violates immutability
        request.setTitle("Updated Title");
        request.setContent("Updated Content");
        request.setChannelType(NotificationChannel.EMAIL);
        request.setIsActive(true);
        request.setVersion(saved.getVersion());

        mockMvc.perform(put("/api/admin/notification-templates/" + saved.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testUpdateTemplate_OptimisticLockConflict() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.save(template);

        UpdateNotificationTemplateRequest request = new UpdateNotificationTemplateRequest();
        request.setTemplateCode("TICKET_CONFIRMATION");
        request.setTitle("Updated Title");
        request.setContent("Updated Content");
        request.setChannelType(NotificationChannel.EMAIL);
        request.setIsActive(true);
        request.setVersion(saved.getVersion() + 99); // stale version

        mockMvc.perform(put("/api/admin/notification-templates/" + saved.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_OPTIMISTIC_LOCK_CONFLICT")));
    }

    @Test
    public void testUpdateStatus_SuccessAndConflict() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.save(template);

        UpdateNotificationTemplateStatusRequest request = new UpdateNotificationTemplateStatusRequest();
        request.setIsActive(false);
        request.setVersion(saved.getVersion());

        mockMvc.perform(patch("/api/admin/notification-templates/" + saved.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.version", is(saved.getVersion() + 1)));

        // Conflict check with old version
        request.setVersion(saved.getVersion()); // version is now version+1, using version will mismatch
        mockMvc.perform(patch("/api/admin/notification-templates/" + saved.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_OPTIMISTIC_LOCK_CONFLICT")));
    }

    @Test
    public void testSecurity_ForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/api/admin/notification-templates")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSecurity_UnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/notification-templates"))
                .andExpect(status().isUnauthorized());
    }
}
