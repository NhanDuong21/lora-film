package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.TicketScanEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketScanEventRepository extends JpaRepository<TicketScanEvent, Long> {
    List<TicketScanEvent> findByEmployeeAccountIdAndScannedAtGreaterThanEqualAndScannedAtLessThanOrderByScannedAtDesc(
            Long employeeAccountId, Instant from, Instant to, Pageable pageable);

    List<TicketScanEvent> findByCinemaPublicIdAndScannedAtGreaterThanEqualAndScannedAtLessThanOrderByScannedAtDesc(
            String cinemaPublicId, Instant from, Instant to, Pageable pageable);
}
