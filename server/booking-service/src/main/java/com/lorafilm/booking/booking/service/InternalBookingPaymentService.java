package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse;

public interface InternalBookingPaymentService {
    InternalPaymentContextResponse getPaymentContext(Long bookingId);
    InternalPaymentContextResponse getPaymentContext(String bookingPublicId);
    InternalPaymentContextResponse getPaymentContextByCode(String bookingCode);
    InternalPaymentContextResponse getScoreRedemptionContext(Long bookingId);
    InternalPaymentContextResponse getScoreRedemptionContext(String bookingPublicId);
    InternalPaymentResultResponse recordPaymentResult(Long bookingId, InternalPaymentResultRequest request);
    InternalPaymentResultResponse recordPaymentResult(String bookingPublicId, InternalPaymentResultRequest request);
    InternalPaymentResultResponse recordRefundResult(
            String bookingPublicId,
            InternalPaymentResultRequest request);
}
