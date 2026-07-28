package com.project.promotionservice.reservation.scheduler;

import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
public class ReservationExpirationScheduler {

    private final PromotionReservationService reservationService;

    public ReservationExpirationScheduler(PromotionReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${promotion.reservation.expiration-delay-ms:30000}")
    public void expireReservations() {
        int expired;
        do {
            expired = reservationService.expireDueReservations("RESERVATION_SCHEDULER");
        } while (expired == 100);
    }
}
