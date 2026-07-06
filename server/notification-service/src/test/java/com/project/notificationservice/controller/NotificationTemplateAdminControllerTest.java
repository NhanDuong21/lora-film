package com.project.notificationservice.controller;

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

    private String adminToken;
    private String userToken;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
        repository.flush();
        adminToken = "Bearer " + generateToken("ADMIN");
        userToken = "Bearer " + generateToken("USER");
    }

    private String generateToken(String role) {
        String secret = "dGVzdF9vbmx5X3NlY3JldF9rZXlfdGhhdF9pc19hdF9sZWFzdF8zMl9ieXRlc19sb25n";
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
        org.springframework.mock.web.MockMultipartFile htmlFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.html",
                MediaType.TEXT_HTML_VALUE,
                "  Xin chào {name}, vé của bạn có mã {bookingCode}.  ".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(htmlFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "  ticket_confirmation  ") // test trim and uppercase normalization
                        .param("title", "  Xác nhận vé LoraFilm  ")
                        .param("channelType", "EMAIL")
                        .param("isActive", "true"))
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

        org.springframework.mock.web.MockMultipartFile htmlFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.html",
                MediaType.TEXT_HTML_VALUE,
                "Content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(htmlFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "ticket_confirmation")
                        .param("title", "Title")
                        .param("channelType", "EMAIL"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_TEMPLATE_CODE_ALREADY_EXISTS")));
    }

    @Test
    public void testCreateTemplate_ValidationFailed() throws Exception {
        org.springframework.mock.web.MockMultipartFile htmlFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.html",
                MediaType.TEXT_HTML_VALUE,
                "Content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(htmlFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "INVALID-CODE-WITH-DASHES!") // pattern validation fail on create
                        .param("title", "") // blank fail
                        .param("channelType", "")) // null/empty fail
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testCreateTemplate_InvalidChannelEnum() throws Exception {
        org.springframework.mock.web.MockMultipartFile htmlFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.html",
                MediaType.TEXT_HTML_VALUE,
                "Hello".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(htmlFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "TICKET_CONFIRMATION")
                        .param("title", "Xác nhận vé LoraFilm")
                        .param("channelType", "INVALID_CHANNEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("NOTIFICATION_INVALID_CHANNEL")));
    }

    @Test
    public void testCreateTemplate_InvalidFileExtension() throws Exception {
        org.springframework.mock.web.MockMultipartFile textFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Some text".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(textFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "TICKET_CONFIRMATION")
                        .param("title", "Title")
                        .param("channelType", "EMAIL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testCreateTemplate_EmptyFile() throws Exception {
        org.springframework.mock.web.MockMultipartFile emptyFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.html",
                MediaType.TEXT_HTML_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(emptyFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "TICKET_CONFIRMATION")
                        .param("title", "Title")
                        .param("channelType", "EMAIL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testCreateTemplate_NonUtf8File() throws Exception {
        byte[] invalidBytes = new byte[]{(byte) 0xC0, (byte) 0xAF}; // invalid UTF-8 sequence
        org.springframework.mock.web.MockMultipartFile invalidFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "template.html",
                MediaType.TEXT_HTML_VALUE,
                invalidBytes
        );

        mockMvc.perform(multipart("/api/admin/notification-templates")
                        .file(invalidFile)
                        .header("Authorization", adminToken)
                        .param("templateCode", "TICKET_CONFIRMATION")
                        .param("title", "Title")
                        .param("channelType", "EMAIL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
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
        NotificationTemplate saved = repository.saveAndFlush(template);
        int initialVersion = saved.getVersion();

        org.springframework.mock.web.MockMultipartFile htmlFile = new org.springframework.mock.web.MockMultipartFile(
                "htmlFile",
                "updated.html",
                MediaType.TEXT_HTML_VALUE,
                "Updated Content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/notification-templates/" + saved.getId())
                        .file(htmlFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", adminToken)
                        .param("templateCode", "TICKET_CONFIRMATION")
                        .param("title", "Updated Title")
                        .param("channelType", "EMAIL")
                        .param("isActive", "true")
                        .param("version", String.valueOf(initialVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Updated Title")))
                .andExpect(jsonPath("$.data.content", is("Updated Content")))
                .andExpect(jsonPath("$.data.version", is(initialVersion + 1)));
    }

    @Test
    public void testUpdateTemplate_StatusOnly() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.saveAndFlush(template);
        int initialVersion = saved.getVersion();

        mockMvc.perform(multipart("/api/admin/notification-templates/" + saved.getId())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", adminToken)
                        .param("isActive", "false")
                        .param("version", String.valueOf(initialVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Title")))
                .andExpect(jsonPath("$.data.content", is("Content")))
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.version", is(initialVersion + 1)));
    }

    @Test
    public void testUpdateTemplate_ImmutableCodeViolation() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.saveAndFlush(template);

        mockMvc.perform(multipart("/api/admin/notification-templates/" + saved.getId())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", adminToken)
                        .param("templateCode", "MODIFIED_CODE") // violates immutability
                        .param("title", "Updated Title")
                        .param("version", String.valueOf(saved.getVersion())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testUpdateTemplate_OptimisticLockConflict() throws Exception {
        NotificationTemplate template = new NotificationTemplate("TICKET_CONFIRMATION", "Title", "Content", NotificationChannel.EMAIL, true);
        NotificationTemplate saved = repository.saveAndFlush(template);

        mockMvc.perform(multipart("/api/admin/notification-templates/" + saved.getId())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", adminToken)
                        .param("templateCode", "TICKET_CONFIRMATION")
                        .param("title", "Updated Title")
                        .param("version", String.valueOf(saved.getVersion() + 99))) // stale version
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }
}
