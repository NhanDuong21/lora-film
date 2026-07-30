package com.project.promotionservice.integration.inbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationEventStateServiceTest {

    @Mock
    private PromotionIntegrationEventRepository repository;

    @Test
    void activeProcessingLeaseCannotBeClaimedByAnotherWorker() {
        PromotionIntegrationEvent event = event(IntegrationEventStatus.PROCESSING);
        event.setUpdatedAt(Instant.now());
        when(repository.findByPublicIdForUpdate("event-1")).thenReturn(Optional.of(event));

        IntegrationEventStateService service =
                new IntegrationEventStateService(repository, 5, 120);

        assertThat(service.markProcessing("event-1")).isFalse();
        verify(repository, never()).save(event);
    }

    @Test
    void expiredProcessingLeaseCanBeRecovered() {
        PromotionIntegrationEvent event = event(IntegrationEventStatus.PROCESSING);
        event.setUpdatedAt(Instant.now().minusSeconds(121));
        when(repository.findByPublicIdForUpdate("event-1")).thenReturn(Optional.of(event));

        IntegrationEventStateService service =
                new IntegrationEventStateService(repository, 5, 120);

        assertThat(service.markProcessing("event-1")).isTrue();
        assertThat(event.getProcessingStatus()).isEqualTo(IntegrationEventStatus.PROCESSING);
        assertThat(event.getUpdatedAt()).isAfter(Instant.now().minusSeconds(5));
        verify(repository).save(event);
    }

    @Test
    void deadLetterCannotBeRetriedWithoutExplicitReset() {
        PromotionIntegrationEvent event = event(IntegrationEventStatus.DEAD_LETTER);
        when(repository.findByPublicIdForUpdate("event-1")).thenReturn(Optional.of(event));

        IntegrationEventStateService service =
                new IntegrationEventStateService(repository, 5, 120);

        assertThat(service.markProcessing("event-1")).isFalse();
        verify(repository, never()).save(event);
    }

    @Test
    void explicitReprocessResetsRetryBudgetAndTerminalMetadata() {
        PromotionIntegrationEvent event = event(IntegrationEventStatus.DEAD_LETTER);
        event.setRetryCount(5);
        event.setLastError("old error");
        event.setProcessedAt(Instant.now());
        when(repository.findByPublicIdForUpdate("event-1")).thenReturn(Optional.of(event));

        IntegrationEventStateService service =
                new IntegrationEventStateService(repository, 5, 120);

        service.resetForReprocess("event-1");

        assertThat(event.getProcessingStatus()).isEqualTo(IntegrationEventStatus.RECEIVED);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getProcessedAt()).isNull();
        verify(repository).save(event);
    }

    private PromotionIntegrationEvent event(IntegrationEventStatus status) {
        PromotionIntegrationEvent event = new PromotionIntegrationEvent();
        event.setPublicId("event-1");
        event.setProcessingStatus(status);
        return event;
    }
}
