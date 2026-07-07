package com.project.notificationservice.service;

import com.project.notificationservice.dto.request.ReferenceDto;
import com.project.notificationservice.dto.request.SendNotificationRequest;
import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.exception.BusinessException;
import com.project.notificationservice.provider.NotificationSenderResolver;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationTemplateRepository;
import com.project.notificationservice.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationValidationTest {

    private NotificationTemplateRepository templateRepository;
    private NotificationLogRepository logRepository;
    private TemplateRenderer templateRenderer;
    private NotificationSenderResolver senderResolver;
    private TransactionTemplate transactionTemplate;
    private NotificationServiceImpl service;

    @BeforeEach
    public void setUp() {
        templateRepository = Mockito.mock(NotificationTemplateRepository.class);
        logRepository = Mockito.mock(NotificationLogRepository.class);
        templateRenderer = new TemplateRenderer();
        senderResolver = Mockito.mock(NotificationSenderResolver.class);
        transactionTemplate = Mockito.mock(TransactionTemplate.class);

        service = new NotificationServiceImpl(
                templateRepository,
                logRepository,
                templateRenderer,
                senderResolver,
                transactionTemplate
        );
    }

    private SendNotificationRequest buildBaseRequest() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRequestSource("test-service");
        request.setUserId(1L);
        request.setEventId("EVT-001");
        request.setChannelType(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setTemplateCode("TEST_TEMPLATE");
        request.setVariables(Map.of("name", "John"));
        return request;
    }

    @Test
    public void testValidation_Success() {
        SendNotificationRequest request = buildBaseRequest();
        // Should pass validation (and then fail due to other mocks, but we verify validation specifically)
        // Let's assert that it doesn't throw a validation error (business exception code VALIDATION_ERROR)
        try {
            service.sendNotification(request);
        } catch (BusinessException e) {
            assertNotEquals("VALIDATION_ERROR", e.getErrorCode());
            assertNotEquals("NOTIFICATION_INVALID_RECIPIENT", e.getErrorCode());
        }
    }

    @Test
    public void testValidation_MissingRequestSource_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setRequestSource(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("requestSource"));
    }

    @Test
    public void testValidation_InvalidUserId_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setUserId(0L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("userId"));
    }

    @Test
    public void testValidation_UnsupportedChannel_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setChannelType(NotificationChannel.SMS);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("NOTIFICATION_CHANNEL_NOT_SUPPORTED", exception.getErrorCode());
    }

    @Test
    public void testValidation_NoIdempotencyKeyOrEventId_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setEventId(null);
        request.setIdempotencyKey(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Either eventId or idempotencyKey"));
    }

    @Test
    public void testValidation_MutuallyExclusive_TemplateAndFreeForm_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setTitle("Freeform Title");
        request.setContent("Freeform Content");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("both templateCode and title/content"));
    }

    @Test
    public void testValidation_InvalidEmailRecipient_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setRecipient("invalid-email");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("NOTIFICATION_INVALID_RECIPIENT", exception.getErrorCode());
    }

    @Test
    public void testValidation_EmailRecipientNewline_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setRecipient("test@example.com\n");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("NOTIFICATION_INVALID_RECIPIENT", exception.getErrorCode());
    }

    @Test
    public void testValidation_ReferenceMismatched_ThrowsException() {
        SendNotificationRequest request = buildBaseRequest();
        request.setReference(new ReferenceDto("BOOKING", null)); // type provided but id is null

        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendNotification(request));
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Reference type and id"));
    }
}
