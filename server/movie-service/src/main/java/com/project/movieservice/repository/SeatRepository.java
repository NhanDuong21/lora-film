package com.project.movieservice.repository;

import com.project.movieservice.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    int countByRoomId(Integer roomId);
}
