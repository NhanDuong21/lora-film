package com.lorafilm.movie.seat.repository;

public interface SeatConflictProjection {
    String getSeatCode();
    Integer getPositionRow();
    Integer getPositionColumn();
}
