package com.lorafilm.booking.controller;

import com.lorafilm.booking.booking.controller.TicketController;
import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.TicketStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingTicketService bookingTicketService;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private TicketController ticketController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
    }

    @Test
    public void getTicketsByBookingId_Success_Returns200() throws Exception {
        BookingTicketDto ticket = new BookingTicketDto();
        ticket.setId(100L);
        ticket.setTicketCode("TK-BK1001-15");
        ticket.setTicketPrice(BigDecimal.valueOf(100000));
        ticket.setStatus(TicketStatus.ACTIVE);

        Booking booking = new Booking();
        booking.setId(10L);
        booking.setPublicId("550e8400-e29b-41d4-a716-446655440000");

        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(booking));
        when(bookingTicketService.findByBooking(10L)).thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/bookings/550e8400-e29b-41d4-a716-446655440000/tickets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ticketCode").value("TK-BK1001-15"));
    }
}
