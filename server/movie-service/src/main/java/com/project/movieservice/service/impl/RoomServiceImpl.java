package com.project.movieservice.service.impl;

import com.project.movieservice.dto.RoomCreateRequest;
import com.project.movieservice.dto.RoomPageResponse;
import com.project.movieservice.dto.RoomResponse;
import com.project.movieservice.dto.RoomStatusUpdateRequest;
import com.project.movieservice.dto.RoomUpdateRequest;
import com.project.movieservice.entity.Room;
import com.project.movieservice.enumtype.RoomStatus;
import com.project.movieservice.enumtype.ScreenType;
import com.project.movieservice.enumtype.ShowtimeStatus;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.RoomRepository;
import com.project.movieservice.repository.SeatRepository;
import com.project.movieservice.repository.ShowtimeRepository;
import com.project.movieservice.service.RoomService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "roomName", "totalSeats", "screenType", "status");

    public RoomServiceImpl(RoomRepository roomRepository, SeatRepository seatRepository, ShowtimeRepository showtimeRepository) {
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public RoomPageResponse getRooms(int page, int size, RoomStatus status, ScreenType screenType, String search, String sort) {
        Sort sortOrder = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (screenType != null) {
                predicates.add(cb.equal(root.get("screenType"), screenType));
            }
            if (search != null && !search.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("roomName")), "%" + search.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Room> roomPage = roomRepository.findAll(spec, pageable);
        
        RoomPageResponse response = new RoomPageResponse();
        response.setContent(roomPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()));
        response.setPage(roomPage.getNumber());
        response.setSize(roomPage.getSize());
        response.setTotalElements(roomPage.getTotalElements());
        response.setTotalPages(roomPage.getTotalPages());
        response.setFirst(roomPage.isFirst());
        response.setLast(roomPage.isLast());
        
        return response;
    }

    @Override
    public RoomResponse getRoomById(Integer roomId) {
        Room room = getRoom(roomId);
        return mapToResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {
        String trimmedName = request.getRoomName().trim();
        if (trimmedName.isEmpty()) {
            throw new BusinessException("Room name cannot be empty", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        if (roomRepository.existsByRoomNameIgnoreCase(trimmedName)) {
            throw new BusinessException("Room name already exists", "ROOM_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        Room room = new Room(trimmedName, request.getTotalSeats(), request.getScreenType(), request.getStatus());
        
        try {
            Room savedRoom = roomRepository.save(room);
            return mapToResponse(savedRoom);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Room name already exists", "ROOM_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Integer roomId, RoomUpdateRequest request) {
        Room room = getRoom(roomId);

        String trimmedName = request.getRoomName().trim();
        if (trimmedName.isEmpty()) {
            throw new BusinessException("Room name cannot be empty", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        if (roomRepository.existsByRoomNameIgnoreCaseAndIdNot(trimmedName, roomId)) {
            throw new BusinessException("Room name already exists", "ROOM_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        int currentSeats = seatRepository.countByRoomId(roomId);
        if (currentSeats > 0 && !request.getTotalSeats().equals(currentSeats)) {
            throw new BusinessException("Total seats update mismatch with existing seats", "ROOM_TOTAL_SEATS_MISMATCH", HttpStatus.CONFLICT);
        }

        // Check future showtime if status is changed to INACTIVE
        if (request.getStatus() == RoomStatus.INACTIVE && room.getStatus() != RoomStatus.INACTIVE) {
            checkFutureShowtimes(roomId);
            validateStatusTransition(room.getStatus(), request.getStatus());
        } else if (room.getStatus() != request.getStatus()) {
            validateStatusTransition(room.getStatus(), request.getStatus());
        }

        room.setRoomName(trimmedName);
        room.setTotalSeats(request.getTotalSeats());
        room.setScreenType(request.getScreenType());
        room.setStatus(request.getStatus());

        try {
            Room savedRoom = roomRepository.save(room);
            return mapToResponse(savedRoom);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Room name already exists", "ROOM_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public void updateRoomStatus(Integer roomId, RoomStatusUpdateRequest request) {
        Room room = getRoom(roomId);
        RoomStatus currentStatus = room.getStatus();
        RoomStatus newStatus = request.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == RoomStatus.INACTIVE) {
            checkFutureShowtimes(roomId);
        }

        room.setStatus(newStatus);
        roomRepository.save(room);
    }

    private Room getRoom(Integer roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("Room not found", "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private void checkFutureShowtimes(Integer roomId) {
        List<ShowtimeStatus> activeStatuses = Arrays.asList(ShowtimeStatus.SCHEDULED, ShowtimeStatus.OPEN, ShowtimeStatus.CLOSED);
        boolean hasFutureShowtime = showtimeRepository.existsByRoomIdAndStartTimeAfterAndStatusIn(roomId, LocalDateTime.now(), activeStatuses);
        
        if (hasFutureShowtime) {
            throw new BusinessException("Room has future showtimes", "ROOM_HAS_FUTURE_SHOWTIMES", HttpStatus.CONFLICT);
        }
    }

    private void validateStatusTransition(RoomStatus currentStatus, RoomStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new BusinessException("Invalid status transition", "ROOM_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        boolean isValid = false;
        if (currentStatus == RoomStatus.ACTIVE && (newStatus == RoomStatus.MAINTENANCE || newStatus == RoomStatus.INACTIVE)) {
            isValid = true;
        } else if (currentStatus == RoomStatus.MAINTENANCE && (newStatus == RoomStatus.ACTIVE || newStatus == RoomStatus.INACTIVE)) {
            isValid = true;
        } else if (currentStatus == RoomStatus.INACTIVE && newStatus == RoomStatus.ACTIVE) {
            isValid = true;
        }

        if (!isValid) {
            throw new BusinessException("Invalid status transition", "ROOM_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }
    }

    private Sort parseSort(String sortStr) {
        if (sortStr == null || sortStr.trim().isEmpty()) {
            return Sort.unsorted();
        }

        String[] parts = sortStr.split(",");
        String field = parts[0].trim();
        
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new BusinessException("Invalid sort field", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }

        return Sort.by(direction, field);
    }

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomName(room.getRoomName());
        response.setTotalSeats(room.getTotalSeats());
        response.setScreenType(room.getScreenType());
        response.setStatus(room.getStatus());
        return response;
    }
}
