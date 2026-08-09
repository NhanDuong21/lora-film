package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.BookingOperationsSummaryResponse;
import com.lorafilm.booking.booking.dto.ManagerCancelBookingRequest;
import com.lorafilm.booking.booking.service.ManagerBookingService;
import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.common.response.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/bookings")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerBookingController {
    private final ManagerBookingService service;

    public ManagerBookingController(ManagerBookingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingAdminResponse>>> search(
            @RequestParam String cinemaPublicId,
            @ModelAttribute BookingFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sách đơn tại rạp", service.search(cinemaPublicId, filter)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BookingOperationsSummaryResponse>> summary(
            @RequestParam String cinemaPublicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tổng quan đơn tại rạp", service.summary(cinemaPublicId)));
    }

    @GetMapping("/{bookingPublicId}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> detail(
            @RequestParam String cinemaPublicId,
            @PathVariable String bookingPublicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Chi tiết đơn tại rạp", service.detail(cinemaPublicId, bookingPublicId)));
    }

    @PutMapping("/{bookingPublicId}/cancel-hold")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> cancelHold(
            @RequestParam String cinemaPublicId,
            @PathVariable String bookingPublicId,
            @Valid @RequestBody ManagerCancelBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã hủy lượt giữ ghế", service.cancelHold(
                        cinemaPublicId, bookingPublicId, request.reason())));
    }
}
