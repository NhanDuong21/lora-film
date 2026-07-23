package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse;

public interface InternalBookingPaymentService {
    InternalPaymentContextResponse getPaymentContext(Long bookingId);
    InternalPaymentResultResponse recordPaymentResult(Long bookingId, InternalPaymentResultRequest request);
}
