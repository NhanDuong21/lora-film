package com.project.movieservice.service;

import com.project.movieservice.dto.RoomCreateRequest;
import com.project.movieservice.dto.RoomResponse;

import com.project.movieservice.entity.Room;
import com.project.movieservice.enumtype.RoomStatus;
import com.project.movieservice.enumtype.ScreenType;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.RoomRepository;
import com.project.movieservice.repository.SeatRepository;
import com.project.movieservice.repository.ShowtimeRepository;
import com.project.movieservice.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("Cinema 1", 100, ScreenType.STANDARD, RoomStatus.ACTIVE);
        room.setId(1);
    }

    @Test
    void testCreateRoom_Success() {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setRoomName("Cinema 1");
        req.setTotalSeats(100);
        req.setScreenType(ScreenType.STANDARD);
        req.setStatus(RoomStatus.ACTIVE);

        when(roomRepository.existsByRoomNameIgnoreCase("Cinema 1")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomResponse res = roomService.createRoom(req);
        assertNotNull(res);
        assertEquals("Cinema 1", res.getRoomName());
    }

    @Test
    void testCreateRoom_DuplicateName() {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setRoomName("Cinema 1");
        req.setTotalSeats(100);
        req.setScreenType(ScreenType.STANDARD);
        req.setStatus(RoomStatus.ACTIVE);

        when(roomRepository.existsByRoomNameIgnoreCase("Cinema 1")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.createRoom(req));
        assertEquals("ROOM_ALREADY_EXISTS", ex.getErrorCode());
    }

    @Test
    void testCreateRoom_DataIntegrityException() {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setRoomName("Cinema 1");
        req.setTotalSeats(100);
        req.setScreenType(ScreenType.STANDARD);
        req.setStatus(RoomStatus.ACTIVE);

        when(roomRepository.existsByRoomNameIgnoreCase("Cinema 1")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.createRoom(req));
        assertEquals("ROOM_ALREADY_EXISTS", ex.getErrorCode());
    }

    @Test
    void testSoftDeleteRoom_Success() {
        when(roomRepository.findById(1)).thenReturn(Optional.of(room)); // ACTIVE
        when(showtimeRepository.existsByRoomIdAndEndTimeAfter(eq(1), any())).thenReturn(false);

        roomService.softDeleteRoom(1);
        assertEquals(RoomStatus.INACTIVE, room.getStatus());
    }

    @Test
    void testSoftDeleteRoom_FutureShowtimes() {
        when(roomRepository.findById(1)).thenReturn(Optional.of(room)); // ACTIVE
        when(showtimeRepository.existsByRoomIdAndEndTimeAfter(eq(1), any())).thenReturn(true);
        
        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.softDeleteRoom(1));
        assertEquals("ROOM_HAS_ACTIVE_SHOWTIMES", ex.getErrorCode());
    }
}
