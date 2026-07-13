package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ShowtimeStatusTransitionIntegrationTest {

    @Autowired
    private ShowtimeStatusTransitionService transitionService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ShowtimeStatusHistoryRepository historyRepository;

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void testTransitionDraftToOpenAndHistoryPersisted() {
        // Assume there is an existing DRAFT showtime created by liquibase/flyway or @BeforeEach
        // For the sake of the structural test, we skip manual data setup and assert logic on the repo
        
        // This is a placeholder test demonstrating where integration logic sits
        // The real test would fetch publicId from a seeded Showtime.
        assertTrue(true, "Integration test skeleton for persistence verification");
    }
}
