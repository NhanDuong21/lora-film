package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.RoomCreateRequest;
import com.project.movieservice.dto.RoomPageResponse;
import com.project.movieservice.dto.RoomResponse;
import com.project.movieservice.dto.RoomStatusUpdateRequest;
import com.project.movieservice.dto.RoomUpdateRequest;
import com.project.movieservice.enumtype.RoomStatus;
import com.project.movieservice.enumtype.ScreenType;
import com.project.movieservice.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RoomPageResponse>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) ScreenType screenType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        
        RoomPageResponse response = roomService.getRooms(page, size, status, screenType, search, sort);
        return ResponseEntity.ok(ApiResponse.success("Rooms retrieved successfully", response));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Integer roomId) {
        RoomResponse response = roomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room retrieved successfully", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room created successfully", response));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Integer roomId,
            @Valid @RequestBody RoomUpdateRequest request) {
        
        RoomResponse response = roomService.updateRoom(roomId, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", response));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Integer roomId) {
        roomService.softDeleteRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }
}
