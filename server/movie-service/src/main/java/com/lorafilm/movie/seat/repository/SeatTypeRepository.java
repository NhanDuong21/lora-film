package com.lorafilm.movie.seat.repository;

import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatTypeRepository extends JpaRepository<SeatType, Long> {
    Optional<SeatType> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByCodeAndDeletedAtIsNull(SeatTypeCode code);
    List<SeatType> findAllByPublicIdInAndDeletedAtIsNull(List<String> publicIds);
}
