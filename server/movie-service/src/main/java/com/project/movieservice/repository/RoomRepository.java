package com.project.movieservice.repository;

import com.project.movieservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer>, JpaSpecificationExecutor<Room> {
    
    boolean existsByRoomNameIgnoreCase(String roomName);
    
    boolean existsByRoomNameIgnoreCaseAndIdNot(String roomName, Integer id);
}
