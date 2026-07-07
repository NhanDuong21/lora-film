package com.project.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookingservice.dto.request.UpdateBookingStatusRequest;
import com.project.bookingservice.dto.response.BookingResponse;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;

@WebMvcTest(controllers = AdminBookingController.class)
@Import({
    com.project.bookingservice.security.SecurityConfig.class,
    com.project.bookingservice.security.JwtFilter.class,
    com.project.bookingservice.security.JwtAuthenticationEntryPoint.class,
    com.project.bookingservice.security.CustomAccessDeniedHandler.class
})
public class AdminBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private com.project.bookingservice.service.IdempotencyService idempotencyService;

    @MockBean
    private com.project.bookingservice.security.JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testSearchBookings_Admin() throws Exception {
        Mockito.when(bookingService.searchBookings(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/admin/bookings")
                .header("Authorization", "Bearer test-token")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testSearchBookings_Employee() throws Exception {
        Mockito.when(bookingService.searchBookings(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/admin/bookings")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSearchBookings_CustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/bookings")
                .header("Authorization", "Bearer test-token")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetBookingDetail() throws Exception {
        BookingResponse response = new BookingResponse();
        Mockito.when(bookingService.getAdminBookingDetail(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/bookings/1")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateBookingStatus() throws Exception {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CANCELLED, "Reason");

        mockMvc.perform(patch("/api/admin/bookings/1/status")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(bookingService).updateBookingStatusAdmin(eq(1L), eq(BookingStatus.CANCELLED), eq("Reason"));
    }
}
