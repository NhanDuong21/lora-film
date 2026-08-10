package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.TicketGateHandoff;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketGateHandoffRepository extends JpaRepository<TicketGateHandoff, Long> {
    Optional<TicketGateHandoff> findByEmployeeAccountIdAndShiftDate(Long employeeAccountId, LocalDate shiftDate);
    List<TicketGateHandoff> findTop10ByEmployeeAccountIdOrderByShiftDateDescHandedOffAtDesc(Long employeeAccountId);
    List<TicketGateHandoff> findTop50ByCinemaPublicIdOrderByShiftDateDescHandedOffAtDesc(String cinemaPublicId);
}
