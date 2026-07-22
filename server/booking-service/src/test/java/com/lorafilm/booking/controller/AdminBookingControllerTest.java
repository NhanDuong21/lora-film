package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.controller.AdminBookingController;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.service.AdminBookingService;
import com.lorafilm.booking.common.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminBookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminBookingService adminBookingService;

    @InjectMocks
    private AdminBookingController adminBookingController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController).build();
    }

    @Test
    public void getBookings_Success_Returns200() throws Exception {
        BookingAdminResponse item = new BookingAdminResponse();
        item.setId(10L);
        item.setBookingCode("BK1001");
        item.setBookingStatus(BookingStatus.PENDING_PAYMENT);

        PagedResponse<BookingAdminResponse> pagedResponse = new PagedResponse<>(
                List.of(item), 0, 20, 1L, 1, true
        );

        when(adminBookingService.findBookings(any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].bookingCode").value("BK1001"));
    }

    @Test
    public void getBookingDetail_Success_Returns200() throws Exception {
        BookingDetailResponse detail = new BookingDetailResponse();
        detail.setId(10L);
        detail.setBookingCode("BK1001");

        when(adminBookingService.getBookingDetail(10L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/bookings/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingCode").value("BK1001"));
    }

    @Test
    public void updateBookingStatus_Success_Returns200() throws Exception {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Paid", "ADMIN", "Updated");
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setBookingCode("BK1001");
        response.setBookingStatus(BookingStatus.CONFIRMED);

        when(adminBookingService.updateBookingStatus(eq(10L), any())).thenReturn(response);

        mockMvc.perform(put("/api/admin/bookings/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"));
    }
}
