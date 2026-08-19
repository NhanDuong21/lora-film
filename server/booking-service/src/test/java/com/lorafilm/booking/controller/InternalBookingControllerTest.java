package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.controller.InternalBookingController;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import com.lorafilm.booking.booking.dto.response.InternalBookingLifecycleResponse;
import com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InternalBookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private InternalBookingService internalBookingService;
    @Mock
    private InternalBookingPaymentService internalBookingPaymentService;

    @InjectMocks
    private InternalBookingController internalBookingController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalBookingController).build();
    }

    @Test
    public void confirmBooking_IsPaymentResultTombstone() {
        com.lorafilm.booking.common.exception.BusinessException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.lorafilm.booking.common.exception.BusinessException.class,
                        () -> internalBookingController.confirmBooking(
                                "550e8400-e29b-41d4-a716-446655440000"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "CONFIRM_VIA_PAYMENT_RESULT_REQUIRED", exception.getErrorCode());
    }

    @Test
    public void expireBooking_Success_Returns200() throws Exception {
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        response.setBookingCode("BK1001");
        response.setBookingStatus(BookingStatus.EXPIRED);

        when(internalBookingService.expireBooking("550e8400-e29b-41d4-a716-446655440000")).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/550e8400-e29b-41d4-a716-446655440000/expire")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("EXPIRED"));
    }

    @Test
    public void refundBooking_IsPaymentResultTombstone() {
        com.lorafilm.booking.common.exception.BusinessException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.lorafilm.booking.common.exception.BusinessException.class,
                        () -> internalBookingController.refundBooking(
                                "550e8400-e29b-41d4-a716-446655440000"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "REFUND_VIA_PAYMENT_RESULT_REQUIRED", exception.getErrorCode());
    }

    @Test
    public void getBookingByCode_Success_Returns200() throws Exception {
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setBookingCode("BK1001");

        when(internalBookingService.getBookingByCode("BK1001")).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/code/BK1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingCode").value("BK1001"));
    }

    @Test
    public void getPaymentContext_Success_ReturnsAuthoritativeAmount() throws Exception {
        InternalPaymentContextResponse response = new InternalPaymentContextResponse(
                10L,
                "550e8400-e29b-41d4-a716-446655440000",
                15L,
                "PENDING_PAYMENT",
                true,
                new BigDecimal("240000.00"),
                "VND",
                Instant.now(),
                Instant.now().plusSeconds(900),
                new InternalPaymentContextResponse.AnalyticsSnapshot(101L, "Superman", 2));
        when(internalBookingPaymentService.getPaymentContext(10L)).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/10/payment-context")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(240000.00))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.analyticsSnapshot.ticketCount").value(2));
    }

    @Test
    public void getLifecycleContext_ReturnsTerminalBookingStatus() throws Exception {
        String publicId = "550e8400-e29b-41d4-a716-446655440000";
        when(internalBookingPaymentService.getLifecycleContext(publicId)).thenReturn(
                new InternalBookingLifecycleResponse(publicId, "LORAFILM-1001", "CANCELLED"));

        mockMvc.perform(get("/internal/bookings/" + publicId + "/lifecycle-context")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingPublicId").value(publicId))
                .andExpect(jsonPath("$.data.bookingCode").value("LORAFILM-1001"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"));
    }

    @Test
    public void getScoreRedemptionContext_DoesNotRequireAmountLock() throws Exception {
        String publicId = "550e8400-e29b-41d4-a716-446655440000";
        InternalPaymentContextResponse response = new InternalPaymentContextResponse(
                10L,
                publicId,
                15L,
                "PENDING_PAYMENT",
                true,
                new BigDecimal("240000.00"),
                "VND",
                null,
                Instant.now().plusSeconds(900),
                new InternalPaymentContextResponse.AnalyticsSnapshot(101L, "Superman", 2));
        when(internalBookingPaymentService.getScoreRedemptionContext(publicId)).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/" + publicId + "/score-redemption-context")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable").value(true))
                .andExpect(jsonPath("$.data.amount").value(240000.00))
                .andExpect(jsonPath("$.data.amountLockedAt").doesNotExist());
    }

    @Test
    public void recordPaymentResult_Success_ReturnsAuthoritativeState() throws Exception {
        String eventId = UUID.randomUUID().toString();
        InternalPaymentResultRequest request = new InternalPaymentResultRequest(
                eventId,
                "1.0",
                501L,
                "660e8400-e29b-41d4-a716-446655440000",
                "TX-1001",
                "VNPAY",
                "VNPAY",
                "SUCCESS",
                new BigDecimal("240000.00"),
                "VND",
                Instant.now(),
                "EXT-999");
        InternalPaymentResultResponse response = new InternalPaymentResultResponse(
                1001L,
                "550e8400-e29b-41d4-a716-446655440000",
                501L,
                "660e8400-e29b-41d4-a716-446655440000",
                eventId,
                "CONFIRMED",
                "SUCCESS",
                true,
                false,
                false,
                null);
        when(internalBookingPaymentService.recordPaymentResult(
                eq(1001L), any(InternalPaymentResultRequest.class))).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/1001/payment-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(eventId))
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.idempotent").value(false));
    }
}
