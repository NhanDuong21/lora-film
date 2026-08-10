package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.seat.dto.AdminSeatLayoutResponse;
import com.lorafilm.movie.seat.dto.CustomerSeatLayoutResponse;

public interface SeatLayoutQueryService {
    CustomerSeatLayoutResponse getCustomerSeatLayout(String auditoriumPublicId);
    AdminSeatLayoutResponse getAdminSeatLayout(String auditoriumPublicId);
}
