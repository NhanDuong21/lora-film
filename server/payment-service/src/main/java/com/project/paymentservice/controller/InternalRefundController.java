package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.ShowtimeRefundTriggerRequest;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments/refunds")
public class InternalRefundController {
    private final RefundService refundService;
    private final String internalToken;

    public InternalRefundController(
            RefundService refundService,
            @Value("${payment.internal-trigger-token:}") String internalToken) {
        this.refundService = refundService;
        this.internalToken = internalToken;
    }

    @PostMapping("/showtimes/{showtimePublicId}")
    public ResponseEntity<ApiResponse<Integer>> refundCancelledShowtime(
            @PathVariable String showtimePublicId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody ShowtimeRefundTriggerRequest request) {
        if (internalToken == null || internalToken.isBlank()
                || token == null || !internalToken.equals(token)) {
            throw new BusinessException(
                    "INTERNAL_TOKEN_INVALID",
                    "Xác thực nội bộ không hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
        int count = refundService.createShowtimeCancellationRefunds(
                showtimePublicId, request.getEventId(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã tiếp nhận hoàn tiền cho suất chiếu bị hủy", count));
    }
}
