package com.project.notificationservice.repository;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.entity.NotificationScheduledJob;
import com.project.notificationservice.entity.NotificationSuppression;
import com.project.notificationservice.entity.WebPushSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ExtendedNotificationPersistenceTest {

    @Autowired
    private NotificationSuppressionRepository suppressionRepository;
    @Autowired
    private WebPushSubscriptionRepository webPushRepository;
    @Autowired
    private NotificationScheduledJobRepository scheduledJobRepository;

    @Test
    void mapsAndQueriesAllExtendedNotificationTables() {
        Instant now = Instant.now();

        NotificationSuppression suppression = new NotificationSuppression();
        suppression.setDestinationHash("a".repeat(64));
        suppression.setChannel(Channel.EMAIL);
        suppression.setReason("BOUNCE");
        suppression.setSource("smtp-provider");
        suppression.setExpiresAt(now.plusSeconds(3600));
        suppressionRepository.saveAndFlush(suppression);

        WebPushSubscription subscription = new WebPushSubscription();
        subscription.setUserPublicId("customer-1");
        subscription.setEndpointEncrypted("encrypted-endpoint");
        subscription.setP256dhEncrypted("encrypted-p256dh");
        subscription.setAuthEncrypted("encrypted-auth");
        webPushRepository.saveAndFlush(subscription);

        NotificationScheduledJob job = new NotificationScheduledJob();
        job.setJobType("MOVIE_REMINDER");
        job.setNextRunAt(now.minusSeconds(1));
        scheduledJobRepository.saveAndFlush(job);

        assertThat(suppressionRepository.findActive(
                "a".repeat(64), Channel.EMAIL, now)).contains(suppression);
        assertThat(webPushRepository.findByUserPublicIdAndActiveTrueOrderByCreatedAtDesc(
                "customer-1")).containsExactly(subscription);
        assertThat(scheduledJobRepository.findDue(
                List.of("SCHEDULED"), now, PageRequest.of(0, 10))).containsExactly(job);
        assertThat(subscription.getPublicId()).matches("[a-f0-9-]{36}");
        assertThat(job.getPublicId()).matches("[a-f0-9-]{36}");
    }
}
