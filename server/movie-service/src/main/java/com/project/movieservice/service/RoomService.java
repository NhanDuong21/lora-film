package com.project.movieservice.service;

import com.project.movieservice.dto.RoomCreateRequest;
import com.project.movieservice.dto.RoomPageResponse;
import com.project.movieservice.dto.RoomResponse;
import com.project.movieservice.dto.RoomStatusUpdateRequest;
import com.project.movieservice.dto.RoomUpdateRequest;
import com.project.movieservice.enumtype.RoomStatus;
import com.project.movieservice.enumtype.ScreenType;

public interface RoomService {
    
    RoomPageResponse getRooms(int page, int size, RoomStatus status, ScreenType screenType, String search, String sort);
    
    RoomResponse getRoomById(Integer roomId);
    
    RoomResponse createRoom(RoomCreateRequest request);
    
    RoomResponse updateRoom(Integer roomId, RoomUpdateRequest request);
    
    void softDeleteRoom(Integer roomId);
}
