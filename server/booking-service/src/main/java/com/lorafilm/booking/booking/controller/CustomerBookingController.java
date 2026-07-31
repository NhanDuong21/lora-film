package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.FinalizeCheckoutRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.response.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.dto.response.BookingSpendingSummaryResponse;
import com.lorafilm.booking.booking.dto.response.BookingSummaryResponse;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.service.BookingService;
import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.common.response.PagedResponse;
import com.lorafilm.booking.common.constant.ValidationConstants;
import com.lorafilm.booking.security.service.SecurityContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

import com.lorafilm.booking.infrastructure.idempotency.Idempotent;

import com.lorafilm.booking.booking.service.BookingTicketService;

@RestController
@RequestMapping("/api/bookings")
@Validated
@Tag(name = "Customer Booking API", description = "Create, list, view and cancel the current user's bookings")
@SecurityRequirement(name = "bearerAuth")
public class CustomerBookingController {

    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "createdAt", "createdAt",
            "bookingCode", "bookingCode",
            "status", "bookingStatus",
            "totalAmount", "finalAmount",
            "expiredAt", "expiresAt"
    );

    private final BookingService bookingService;
    private final SecurityContextService securityContextService;
    private final BookingTicketService bookingTicketService;

    public CustomerBookingController(
            BookingService bookingService,
            SecurityContextService securityContextService,
            BookingTicketService bookingTicketService) {
        this.bookingService = bookingService;
        this.securityContextService = securityContextService;
        this.bookingTicketService = bookingTicketService;
    }

    @PostMapping
    @Operation(summary = "Create booking", description = "Creates a PENDING_PAYMENT booking from active reservations")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Booking created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid request or showtime", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Authentication required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Reservation expired or already converted", content = @Content)
    })
    @Idempotent
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get my bookings", description = "Returns a filtered and paginated list for the authenticated user")
    public ResponseEntity<ApiResponse<PagedResponse<BookingSummaryResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Sort in field,direction format", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Long currentUserId = securityContextService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<BookingSummaryResponse> bookings = bookingService.findByUser(
                currentUserId, status, fromDate, toDate, pageable);
        PagedResponse<BookingSummaryResponse> response = new PagedResponse<>(
                bookings.getContent(),
                bookings.getNumber(),
                bookings.getSize(),
                bookings.getTotalElements(),
                bookings.getTotalPages(),
                bookings.isLast());
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", response));
    }

    @GetMapping("/spending-summary")
    @Operation(
            summary = "Get my annual paid spending",
            description = "Sums successful CONFIRMED or COMPLETED bookings in the requested calendar year")
    public ResponseEntity<ApiResponse<BookingSpendingSummaryResponse>> getMySpendingSummary(
            @RequestParam @Min(2000) @Max(2100) int year) {
        return ResponseEntity.ok(ApiResponse.success(
                "Spending summary retrieved successfully",
                bookingService.getMySpendingSummary(year)));
    }

    @GetMapping("/active")
    @Operation(
            summary = "Get my active booking for a showtime",
            description = "Returns the unexpired PENDING_PAYMENT booking that prevents creating a second order")
    public ResponseEntity<ApiResponse<BookingResponse>> getActiveBooking(
            @RequestParam
            @Pattern(regexp = ValidationConstants.UUID_PATTERN,
                    message = "showtimePublicId must be a valid UUID")
            String showtimePublicId) {
        BookingResponse activeBooking = bookingService
                .findActiveByShowtime(showtimePublicId)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                activeBooking == null
                        ? "Không có đơn giữ ghế đang hoạt động cho suất chiếu này"
                        : "Đã tải đơn giữ ghế đang hoạt động",
                activeBooking));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get booking detail", description = "Only the booking owner or an administrator can view it")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking retrieved successfully", bookingService.findById(publicId)));
    }

    @GetMapping("/code/{bookingCode}")
    @Operation(summary = "Find booking by code", description = "Only the booking owner or an administrator can view it")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingByCode(
            @PathVariable String bookingCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking retrieved successfully", bookingService.findByCode(bookingCode)));
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Cancel booking", description = "Only a PENDING_PAYMENT booking can be cancelled")
    @Idempotent
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId,
            @Valid @RequestBody(required = false) CancelBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking cancelled successfully", bookingService.cancelBooking(publicId, request)));
    }

    @PostMapping("/{publicId}/finalize-checkout")
    @Operation(summary = "Lock the checkout amount", description = "Locks the server-owned amount before Payment handoff")
    public ResponseEntity<ApiResponse<BookingResponse>> finalizeCheckout(
            @PathVariable String publicId,
            @Valid @RequestBody(required = false) FinalizeCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Checkout finalized successfully", bookingService.finalizeCheckout(publicId, request)));
    }

    @PostMapping("/{publicId}/payment")
    @Operation(summary = "Deprecated payment initiation", description = "Payment is owned by Payment Service after checkout finalization")
    @Idempotent
    public ResponseEntity<ApiResponse<Void>> initiatePayment(
            @PathVariable
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId,
            @RequestBody(required = false) Map<String, Object> ignoredRequest) {
        throw new com.lorafilm.booking.common.exception.BusinessException(
                "PAYMENT_SERVICE_HANDOFF_REQUIRED",
                "Payment initiation is owned by Payment Service",
                HttpStatus.GONE);
    }


    @PostMapping("/{publicId}/resend-email")
    @Operation(summary = "Resend booking email", description = "Resends the email confirmation for a confirmed booking")
    public ResponseEntity<ApiResponse<Void>> resendEmail(
            @PathVariable
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        BookingDetailResponse detail = bookingService.findById(publicId);
        if (!BookingStatus.CONFIRMED.equals(detail.status()) && !BookingStatus.COMPLETED.equals(detail.status())) {
            throw new com.lorafilm.booking.common.exception.BusinessException(
                    "INVALID_BOOKING_STATUS",
                    "Email can only be resent for CONFIRMED or COMPLETED bookings",
                    HttpStatus.BAD_REQUEST);
        }
        bookingTicketService.resendBookingEmail(publicId);
        return ResponseEntity.ok(ApiResponse.success("Email resent successfully", null));
    }


    private Sort parseSort(String sortValue) {
        String[] parts = sortValue == null ? new String[0] : sortValue.split(",", -1);
        String requestedProperty = parts.length > 0 ? parts[0].trim() : "createdAt";
        String property = SORT_PROPERTIES.get(requestedProperty);
        if (property == null) {
            throw new IllegalArgumentException(
                    "Unsupported sort field. Allowed fields: " + String.join(", ", SORT_PROPERTIES.keySet()));
        }
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromString(parts[1].trim())
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
