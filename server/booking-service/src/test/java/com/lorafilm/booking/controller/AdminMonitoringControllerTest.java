package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.infrastructure.repository.BookingRetryTaskRepository;
import com.lorafilm.booking.monitoring.controller.AdminMonitoringController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminMonitoringControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingRetryTaskRepository retryTaskRepository;

    @InjectMocks
    private AdminMonitoringController adminMonitoringController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminMonitoringController).build();
    }

    @Test
    public void getMonitoringSummary_Success_Returns200AndCorrectFields() throws Exception {
        when(bookingRepository.countByCreatedAtAfter(any())).thenReturn(100L);
        when(bookingRepository.countByPaymentStatus(any())).thenReturn(5L);
        when(bookingRepository.countByBookingStatus(any())).thenReturn(12L);
        when(retryTaskRepository.countByStatus(any())).thenReturn(3L);

        mockMvc.perform(get("/api/admin/monitoring/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingToday").value(100))
                .andExpect(jsonPath("$.data.paymentFailed").value(5))
                .andExpect(jsonPath("$.data.expiredBooking").value(12))
                .andExpect(jsonPath("$.data.pendingRetry").value(3));
    }
}
