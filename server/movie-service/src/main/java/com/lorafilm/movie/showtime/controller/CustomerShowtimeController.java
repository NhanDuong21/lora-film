package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.showtime.dto.response.CustomerSeatLayoutResponse;
import com.lorafilm.movie.showtime.service.CustomerShowtimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/showtimes")
public class CustomerShowtimeController {
    private final CustomerShowtimeService customerShowtimeService;

    public CustomerShowtimeController(CustomerShowtimeService customerShowtimeService) {
        this.customerShowtimeService = customerShowtimeService;
    }

    @GetMapping("/{showtimePublicId}/seat-layout")
    public ApiResponse<CustomerSeatLayoutResponse> getSeatLayout(
            @PathVariable String showtimePublicId) {
        return ApiResponse.ok(customerShowtimeService.getSeatLayout(showtimePublicId));
    }
}
