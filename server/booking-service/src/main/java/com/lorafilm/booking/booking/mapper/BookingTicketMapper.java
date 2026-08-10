package com.lorafilm.booking.booking.mapper;

import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.dto.CreateTicketRequest;
import com.lorafilm.booking.booking.entity.BookingTicket;
import com.lorafilm.booking.booking.enums.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BookingTicketMapper {

    public BookingTicketDto toDto(BookingTicket ticket) {
        if (ticket == null) {
            return null;
        }

        BookingTicketDto dto = new BookingTicketDto();
        dto.setId(ticket.getId());
        dto.setPublicId(ticket.getPublicId());
        if (ticket.getBooking() != null) {
            dto.setBookingId(ticket.getBooking().getId());
        }
        dto.setTicketCode(ticket.getTicketCode());
        dto.setSeatId(ticket.getSeatId());
        dto.setSeatLabel(ticket.getSeatLabel());
        dto.setSeatRow(ticket.getSeatRow());
        dto.setSeatColumn(ticket.getSeatColumn());
        dto.setSeatType(ticket.getSeatType());
        dto.setTicketPrice(ticket.getTicketPrice());
        dto.setMovieTitle(ticket.getMovieTitle());
        dto.setCinemaName(ticket.getCinemaName());
        dto.setAuditoriumName(ticket.getAuditoriumName());
        dto.setShowtimeStart(ticket.getShowtimeStart());
        dto.setShowtimeEnd(ticket.getShowtimeEnd());
        dto.setMovieFormat(ticket.getMovieFormat());
        dto.setAudioLanguage(ticket.getAudioLanguage());
        dto.setSubtitleLanguage(ticket.getSubtitleLanguage());
        dto.setQrCode(ticket.getQrCode());
        dto.setBarcode(ticket.getBarcode());
        dto.setStatus(ticket.getStatus());
        dto.setUsedAt(ticket.getUsedAt());
        dto.setCreatedAt(ticket.getCreatedAt());
        return dto;
    }

    public BookingTicket toEntity(CreateTicketRequest request) {
        if (request == null) {
            return null;
        }

        BookingTicket ticket = new BookingTicket();
        ticket.setPublicId(UUID.randomUUID().toString());
        ticket.setSeatId(request.getSeatId());
        ticket.setSeatLabel(request.getSeatLabel());
        ticket.setSeatRow(request.getSeatRow());
        ticket.setSeatColumn(request.getSeatColumn());
        ticket.setSeatType(request.getSeatType());
        ticket.setTicketPrice(request.getTicketPrice());
        ticket.setMovieTitle(request.getMovieTitle());
        ticket.setCinemaName(request.getCinemaName());
        ticket.setAuditoriumName(request.getAuditoriumName());
        ticket.setShowtimeStart(request.getShowtimeStart());
        ticket.setShowtimeEnd(request.getShowtimeEnd());
        ticket.setMovieFormat(request.getMovieFormat());
        ticket.setAudioLanguage(request.getAudioLanguage());
        ticket.setSubtitleLanguage(request.getSubtitleLanguage());
        ticket.setStatus(TicketStatus.ACTIVE);
        return ticket;
    }
}
