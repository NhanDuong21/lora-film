package com.project.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import com.project.notificationservice.domain.NotificationTypes.RequestStatus;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;
import com.project.notificationservice.entity.NotificationDelivery;
import com.project.notificationservice.entity.NotificationRecipient;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.provider.NotificationChannelSender;
import com.project.notificationservice.provider.NotificationChannelSender.DeliveryResult;
import com.project.notificationservice.provider.NotificationChannelSender.RenderedNotification;
import com.project.notificationservice.provider.NotificationSenderResolver;
import com.project.notificationservice.repository.NotificationDeadLetterRepository;
import com.project.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationOutboxRepository;
import com.project.notificationservice.repository.NotificationRecipientRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import com.project.notificationservice.template.SafeTemplateRenderer;
import com.project.notificationservice.template.TemplatePayloadAdapter;
import com.project.notificationservice.template.TemplateRegistry;
import com.project.notificationservice.template.TemplateRegistry.RenderedTemplate;
import com.project.notificationservice.template.TemplateRegistry.TemplateDocument;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryWorkerTest {

    @Test
    void expiredRequestCancelsDeliveryAndRequestInsteadOfLeavingRequestProcessing() {
        NotificationRequestRepository requestRepository = mock(NotificationRequestRepository.class);
        NotificationRecipientRepository recipientRepository = mock(NotificationRecipientRepository.class);
        NotificationDeliveryRepository deliveryRepository = mock(NotificationDeliveryRepository.class);
        NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);
        NotificationDeadLetterRepository deadLetterRepository = mock(NotificationDeadLetterRepository.class);
        NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);

        NotificationRequest request = new NotificationRequest();
        ReflectionTestUtils.setField(request, "id", 10L);
        request.setPublicId("request-expired");
        request.setStatus(RequestStatus.PROCESSING);
        request.setExpiresAt(Instant.now().minusSeconds(1));

        NotificationRecipient recipient = new NotificationRecipient();
        ReflectionTestUtils.setField(recipient, "id", 20L);

        NotificationDelivery delivery = new NotificationDelivery();
        ReflectionTestUtils.setField(delivery, "id", 30L);
        ReflectionTestUtils.setField(delivery, "publicId", "delivery-expired");
        delivery.setNotificationRequestId(10L);
        delivery.setNotificationRecipientId(20L);
        delivery.setChannel(Channel.EMAIL);
        delivery.setStatus(DeliveryStatus.RETRY_SCHEDULED);
        delivery.setNextRetryAt(Instant.now().minusSeconds(1));

        when(deliveryRepository.findDue(any(), any(), any())).thenReturn(List.of(delivery));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(recipientRepository.findById(20L)).thenReturn(Optional.of(recipient));

        NotificationDeliveryWorker worker = new NotificationDeliveryWorker(
                requestRepository, recipientRepository, deliveryRepository, attemptRepository,
                deadLetterRepository, outboxRepository, mock(TemplateRegistry.class),
                mock(SafeTemplateRenderer.class), mock(TemplatePayloadAdapter.class),
                mock(NotificationSenderResolver.class), mock(RecipientCryptoService.class),
                new ObjectMapper(), new SimpleMeterRegistry(), 10, 5);

        worker.deliverDueNotifications();

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        verify(outboxRepository).save(any());
    }

    @Test
    void retryReusesTheFirstRenderedSnapshotInsteadOfResolvingTheLatestTemplate() {
        NotificationRequestRepository requestRepository = mock(NotificationRequestRepository.class);
        NotificationRecipientRepository recipientRepository = mock(NotificationRecipientRepository.class);
        NotificationDeliveryRepository deliveryRepository = mock(NotificationDeliveryRepository.class);
        NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);
        NotificationDeadLetterRepository deadLetterRepository = mock(NotificationDeadLetterRepository.class);
        NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
        TemplateRegistry templateRegistry = mock(TemplateRegistry.class);
        SafeTemplateRenderer renderer = mock(SafeTemplateRenderer.class);
        TemplatePayloadAdapter payloadAdapter = mock(TemplatePayloadAdapter.class);
        NotificationSenderResolver senderResolver = mock(NotificationSenderResolver.class);
        RecipientCryptoService cryptoService = mock(RecipientCryptoService.class);
        NotificationChannelSender sender = mock(NotificationChannelSender.class);

        NotificationRequest request = new NotificationRequest();
        ReflectionTestUtils.setField(request, "id", 10L);
        request.setPublicId("request-1");
        request.setTemplateKey("REGISTER_OTP");
        request.setLocale("vi-VN");
        request.setCategory(Category.SECURITY);
        request.setStatus(RequestStatus.ACCEPTED);
        request.setPayloadJson("{}");

        NotificationRecipient recipient = new NotificationRecipient();
        ReflectionTestUtils.setField(recipient, "id", 20L);
        recipient.setUserPublicId("user-1");

        NotificationDelivery delivery = new NotificationDelivery();
        ReflectionTestUtils.setField(delivery, "id", 30L);
        ReflectionTestUtils.setField(delivery, "publicId", "delivery-1");
        delivery.setNotificationRequestId(10L);
        delivery.setNotificationRecipientId(20L);
        delivery.setChannel(Channel.IN_APP);
        delivery.setProvider("in-app");
        delivery.setStatus(DeliveryStatus.PENDING);

        TemplateDocument template = new TemplateDocument(
                "REGISTER_OTP", "Register otp", "", Category.SECURITY,
                Channel.IN_APP, "vi-VN", TemplateStatus.PUBLISHED,
                Map.of(), Map.of(), "Subject v1", "<p>HTML v1</p>", "Text v1",
                "commit-v1", "1", Instant.now());
        RenderedTemplate rendered = new RenderedTemplate(
                "Rendered subject v1", "<p>Rendered HTML v1</p>", "Rendered text v1");

        when(deliveryRepository.findDue(any(), any(), any())).thenReturn(List.of(delivery));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(recipientRepository.findById(20L)).thenReturn(Optional.of(recipient));
        when(deliveryRepository.findByNotificationRequestIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(delivery));
        when(templateRegistry.getPublishedTemplate("REGISTER_OTP", Channel.IN_APP, "vi-VN"))
                .thenReturn(template);
        when(payloadAdapter.adapt(any(), any())).thenReturn(Map.of());
        when(renderer.render(template, Map.of())).thenReturn(rendered);
        when(senderResolver.resolve(Channel.IN_APP)).thenReturn(sender);
        when(sender.send(any())).thenReturn(DeliveryResult.failure(
                "in-app", FailureCategory.TRANSIENT, "TEMPORARY", "Retry", 0L));

        NotificationDeliveryWorker worker = new NotificationDeliveryWorker(
                requestRepository, recipientRepository, deliveryRepository, attemptRepository,
                deadLetterRepository, outboxRepository, templateRegistry, renderer, payloadAdapter,
                senderResolver, cryptoService, new ObjectMapper(), new SimpleMeterRegistry(), 10, 5);

        worker.deliverDueNotifications();
        worker.deliverDueNotifications();

        verify(templateRegistry, times(1))
                .getPublishedTemplate("REGISTER_OTP", Channel.IN_APP, "vi-VN");
        verify(renderer, times(1)).render(template, Map.of());
        verify(sender, times(2)).send(any(RenderedNotification.class));
        assertThat(delivery.getTemplateCommitSha()).isEqualTo("commit-v1");
        assertThat(delivery.getRenderedSubject()).isEqualTo("Rendered subject v1");
        assertThat(delivery.getRenderedHtml()).isEqualTo("<p>Rendered HTML v1</p>");
        assertThat(delivery.getRenderedText()).isEqualTo("Rendered text v1");
    }
}
