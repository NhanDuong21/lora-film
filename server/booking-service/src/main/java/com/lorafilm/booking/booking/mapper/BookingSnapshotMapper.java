package com.lorafilm.booking.booking.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.CreateSnapshotRequest;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BookingSnapshotMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public BookingSnapshotDto toDto(BookingSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        BookingSnapshotDto dto = new BookingSnapshotDto();
        dto.setId(snapshot.getId());
        dto.setPublicId(snapshot.getPublicId());
        if (snapshot.getBooking() != null) {
            dto.setBookingId(snapshot.getBooking().getId());
        }
        dto.setMovieId(snapshot.getMovieId());
        dto.setMovieTitle(snapshot.getMovieTitle());
        dto.setOriginalTitle(snapshot.getOriginalTitle());
        dto.setMoviePoster(snapshot.getMoviePoster());
        dto.setDuration(snapshot.getDuration());
        dto.setAgeRating(snapshot.getAgeRating());
        dto.setShowtimeId(snapshot.getShowtimeId());
        dto.setShowtimeStart(snapshot.getShowtimeStart());
        dto.setShowtimeEnd(snapshot.getShowtimeEnd());
        dto.setCinemaId(snapshot.getCinemaId());
        dto.setCinemaName(snapshot.getCinemaName());
        dto.setAuditoriumId(snapshot.getAuditoriumId());
        dto.setAuditoriumName(snapshot.getAuditoriumName());
        dto.setSeatCount(snapshot.getSeatCount());
        dto.setSeats(readSeats(snapshot.getSnapshotJson()));
        dto.setPromotionCode(snapshot.getPromotionCode());
        dto.setPromotionName(snapshot.getPromotionName());
        dto.setSnapshotJson(snapshot.getSnapshotJson());
        dto.setCreatedAt(snapshot.getCreatedAt());
        return dto;
    }

    public BookingSnapshot toEntity(CreateSnapshotRequest request) {
        if (request == null) {
            return null;
        }

        BookingSnapshot snapshot = new BookingSnapshot();
        snapshot.setPublicId(UUID.randomUUID().toString());
        snapshot.setMovieId(request.getMovieId());
        snapshot.setMovieTitle(request.getMovieTitle());
        snapshot.setOriginalTitle(request.getOriginalTitle());
        snapshot.setMoviePoster(request.getMoviePoster());
        snapshot.setDuration(request.getDuration());
        snapshot.setAgeRating(request.getAgeRating());
        snapshot.setShowtimeId(request.getShowtimeId());
        snapshot.setShowtimeStart(request.getShowtimeStart());
        snapshot.setShowtimeEnd(request.getShowtimeEnd());
        snapshot.setCinemaId(request.getCinemaId());
        snapshot.setCinemaName(request.getCinemaName());
        snapshot.setAuditoriumId(request.getAuditoriumId());
        snapshot.setAuditoriumName(request.getAuditoriumName());
        snapshot.setSeatCount(request.getSeatCount());
        snapshot.setPromotionCode(request.getPromotionCode());
        snapshot.setPromotionName(request.getPromotionName());
        snapshot.setSnapshotJson(request.getSnapshotJson());
        return snapshot;
    }

    private List<BookingSnapshotDto.SeatSnapshot> readSeats(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(
                    snapshotJson,
                    new TypeReference<List<BookingSnapshotDto.SeatSnapshot>>() {
                    });
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
