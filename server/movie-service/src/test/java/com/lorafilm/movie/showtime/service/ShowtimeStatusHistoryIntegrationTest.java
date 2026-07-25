package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeStatusHistory;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.dto.response.ShowtimeStatusHistoryResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class ShowtimeStatusHistoryIntegrationTest {

    @Autowired
    private ShowtimeStatusHistoryRepository historyRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private ShowtimeStatusHistoryService historyService;

    @Test
    void testPersistHistorySuccess() {
        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle("Test Movie");
        movie.setSlug("test-movie");
        movie.setDurationMinutes(120);
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.P);
        movie.setReleaseDate(java.time.LocalDate.now());
        movie.setStatus(com.lorafilm.movie.movie.domain.enums.MovieStatus.NOW_SHOWING);
        movie = movieRepository.save(movie);

        MovieVersion version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName("2D Sub");
        version.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        version.setAudioLanguage("EN");
        version.setSubtitleLanguage("VI");
        version.setStatus(com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE);
        version = movieVersionRepository.save(version);

        Cinema cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setName("Test Cinema");
        cinema.setSlug("test-cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema = cinemaRepository.save(cinema);

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName("Test Aud");
        auditorium.setScreenType(com.lorafilm.movie.auditorium.domain.enums.ScreenType.STANDARD);
        auditorium.setSoundType(com.lorafilm.movie.auditorium.domain.enums.SoundType.STANDARD);
        auditorium.setCapacity(100);
        auditorium.setStatus(com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.DRAFT);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.save(auditorium);

        Showtime showtime = new Showtime();
        showtime.setPublicId(UUID.randomUUID().toString());
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(Instant.now().plusSeconds(3600));
        showtime.setEndTime(Instant.now().plusSeconds(7200));
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setSource(ShowtimeSource.AUTO);
        showtime.setBatchId("preview-source-1");
        showtime = showtimeRepository.save(showtime);

        ShowtimeStatusHistory history = new ShowtimeStatusHistory();
        history.setShowtime(showtime);
        history.setPreviousStatus(null);
        history.setNewStatus(ShowtimeStatus.DRAFT);
        history.setReason("Showtime created");
        history.setChangedBy(99L);
        
        history = historyRepository.save(history);

        ShowtimeStatusHistory transition = new ShowtimeStatusHistory();
        transition.setShowtime(showtime);
        transition.setPreviousStatus(ShowtimeStatus.DRAFT);
        transition.setNewStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        transition.setReason(null);
        transition.setChangedBy(null);
        transition.setChangedAt(Instant.now().plusSeconds(1));
        historyRepository.save(transition);
        historyRepository.flush();

        assertThat(history.getId()).isNotNull();
        assertThat(history.getPreviousStatus()).isNull();
        assertThat(history.getNewStatus()).isEqualTo(ShowtimeStatus.DRAFT);
        assertThat(history.getReason()).isEqualTo("Showtime created");
        assertThat(history.getChangedAt()).isNotNull();
        assertThat(history.getChangedBy()).isEqualTo(99L);

        List<ShowtimeStatusHistoryResponse> responses =
                historyService.getShowtimeStatusHistory(showtime.getPublicId());
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getSource()).isEqualTo("AUTO");
        assertThat(responses.get(0).getPreviewPublicId()).isEqualTo("preview-source-1");
        assertThat(responses.get(1).getSource()).isEqualTo("MANUAL");
        assertThat(responses.get(1).getPreviewPublicId()).isEqualTo("preview-source-1");
        assertThat(responses.get(1).getChangedBy()).isNull();
        assertThat(responses.get(1).getReason()).isNull();
    }
}
