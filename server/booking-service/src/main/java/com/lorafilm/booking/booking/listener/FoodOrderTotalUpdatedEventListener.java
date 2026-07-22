package com.lorafilm.booking.booking.listener;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.food.event.FoodOrderTotalUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FoodOrderTotalUpdatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderTotalUpdatedEventListener.class);

    private final BookingRepository bookingRepository;

    public FoodOrderTotalUpdatedEventListener(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @EventListener
    @Transactional
    public void onFoodOrderTotalUpdated(FoodOrderTotalUpdatedEvent event) {
        log.info("Received FoodOrderTotalUpdatedEvent for Booking: {}, new Food Amount: {}",
                event.getBookingPublicId(), event.getFinalAmount());

        Booking booking = bookingRepository.findByPublicIdWithLock(event.getBookingPublicId())
                .orElse(null);

        if (booking != null) {
            booking.setFoodAmount(event.getFinalAmount());
            booking.recalculateFinalAmount();
            bookingRepository.save(booking);
            log.info("Successfully updated Booking {} with new food amount {}", booking.getPublicId(), event.getFinalAmount());
        } else {
            log.warn("Booking {} not found while processing FoodOrderTotalUpdatedEvent", event.getBookingPublicId());
        }
    }
}
