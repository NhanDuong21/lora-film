package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.ticketscan.TicketCheckerSummaryResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketGateHandoffRequest;
import com.lorafilm.booking.booking.dto.ticketscan.TicketGateHandoffResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketScanRequest;
import com.lorafilm.booking.booking.dto.ticketscan.TicketScanResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketShowtimeOperationResponse;
import com.lorafilm.booking.booking.enums.TicketScanResult;
import com.lorafilm.booking.booking.service.TicketCheckerService;
import com.lorafilm.booking.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/ticket-operations")
@PreAuthorize("hasAuthority('TICKET_SCAN')")
public class EmployeeTicketCheckerController {

    private final TicketCheckerService service;

    public EmployeeTicketCheckerController(TicketCheckerService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<TicketScanResponse>> scan(@Valid @RequestBody TicketScanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã kiểm tra vé", service.scan(request)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TicketCheckerSummaryResponse>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Tổng quan soát vé", service.summary(date)));
    }

    @GetMapping("/showtimes")
    public ResponseEntity<ApiResponse<List<TicketShowtimeOperationResponse>>> showtimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Các suất chiếu tại cửa soát", service.showtimes(date)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TicketScanResponse>>> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) TicketScanResult result) {
        return ResponseEntity.ok(ApiResponse.success("Lịch sử soát vé", service.history(date, result)));
    }

    @PostMapping("/handoffs")
    public ResponseEntity<ApiResponse<TicketGateHandoffResponse>> handoff(
            @Valid @RequestBody TicketGateHandoffRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu bàn giao cửa soát", service.handoff(request, date)));
    }

    @GetMapping("/handoffs")
    public ResponseEntity<ApiResponse<List<TicketGateHandoffResponse>>> handoffs() {
        return ResponseEntity.ok(ApiResponse.success("Lịch sử bàn giao cửa soát", service.handoffHistory()));
    }
}
