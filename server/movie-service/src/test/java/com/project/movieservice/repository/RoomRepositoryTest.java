package com.project.movieservice.repository;

import com.project.movieservice.entity.Room;
import com.project.movieservice.enumtype.RoomStatus;
import com.project.movieservice.enumtype.ScreenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    public void testExistsByRoomNameIgnoreCase() {
        Room room = new Room("Cinema 1", 100, ScreenType.STANDARD, RoomStatus.ACTIVE);
        roomRepository.save(room);

        assertTrue(roomRepository.existsByRoomNameIgnoreCase("cinema 1"));
        assertFalse(roomRepository.existsByRoomNameIgnoreCase("cinema 2"));
    }

    @Test
    public void testExistsByRoomNameIgnoreCaseAndIdNot() {
        Room room1 = new Room("Cinema 1", 100, ScreenType.STANDARD, RoomStatus.ACTIVE);
        Room room2 = new Room("Cinema 2", 100, ScreenType.STANDARD, RoomStatus.ACTIVE);
        room1 = roomRepository.save(room1);
        room2 = roomRepository.save(room2);

        assertTrue(roomRepository.existsByRoomNameIgnoreCaseAndIdNot("cinema 2", room1.getId()));
        assertFalse(roomRepository.existsByRoomNameIgnoreCaseAndIdNot("cinema 1", room1.getId()));
    }
}
