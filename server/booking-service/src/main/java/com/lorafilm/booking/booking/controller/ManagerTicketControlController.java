package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.ticketscan.TicketCheckerSummaryResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketGateHandoffResponse;
import com.lorafilm.booking.booking.dto.ticketscan.TicketScanResponse;
import com.lorafilm.booking.booking.enums.TicketScanResult;
import com.lorafilm.booking.booking.service.TicketCheckerService;
import com.lorafilm.booking.common.response.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/ticket-operations")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerTicketControlController {

    private final TicketCheckerService service;

    public ManagerTicketControlController(TicketCheckerService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TicketCheckerSummaryResponse>> summary(
            @RequestParam String cinemaPublicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tổng quan kiểm soát vé tại rạp", service.managerSummary(cinemaPublicId, date)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TicketScanResponse>>> history(
            @RequestParam String cinemaPublicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) TicketScanResult result) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lịch sử soát vé tại rạp", service.managerHistory(cinemaPublicId, date, result)));
    }

    @GetMapping("/handoffs")
    public ResponseEntity<ApiResponse<List<TicketGateHandoffResponse>>> handoffs(
            @RequestParam String cinemaPublicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                "Biên bản bàn giao cửa soát vé", service.managerHandoffHistory(cinemaPublicId, date)));
    }
}
