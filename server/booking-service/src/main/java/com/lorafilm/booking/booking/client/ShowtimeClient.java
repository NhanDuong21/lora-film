package com.lorafilm.booking.booking.client;

import java.util.List;

public interface ShowtimeClient {

    ShowtimeBookingContext getBookingContext(Long showtimeId, List<Long> seatIds);
}
