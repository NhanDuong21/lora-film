package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieExhibitionPeriod;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.CreateMovieExhibitionPeriodRequest;
import com.lorafilm.movie.movie.dto.MovieExhibitionPeriodResponse;
import com.lorafilm.movie.movie.repository.MovieExhibitionPeriodRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MovieExhibitionPeriodService {

    private final MovieRepository movieRepository;
    private final MovieExhibitionPeriodRepository periodRepository;
    private final MovieOperationalGuard operationalGuard;
    private final Clock clock;

    public MovieExhibitionPeriodService(MovieRepository movieRepository,
                                        MovieExhibitionPeriodRepository periodRepository,
                                        MovieOperationalGuard operationalGuard,
                                        Clock clock) {
        this.movieRepository = movieRepository;
        this.periodRepository = periodRepository;
        this.operationalGuard = operationalGuard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MovieExhibitionPeriodResponse> getPeriods(String moviePublicId) {
        Movie movie = findMovie(moviePublicId);
        return periodRepository.findByMovieIdAndDeletedAtIsNullOrderByStartDateDescIdDesc(movie.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MovieExhibitionPeriodResponse createPeriod(
            String moviePublicId,
            CreateMovieExhibitionPeriodRequest request) {
        Movie movie = findMovie(moviePublicId);
        validateDates(request.getStartDate(), request.getEndDate());
        if (movie.getStatus() != MovieStatus.DRAFT && movie.getStatus() != MovieStatus.ENDED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ có thể lập đợt khai thác mới cho phim Chờ hoàn thiện hoặc Đã kết thúc.");
        }
        if (Objects.equals(movie.getReleaseDate(), request.getStartDate())
                && Objects.equals(movie.getEndDate(), request.getEndDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Khoảng thời gian này đang là đợt khai thác hiện tại của phim.");
        }

        operationalGuard.assertReleaseWindowEditable(movie, request.getStartDate(), request.getEndDate());
        preserveCurrentPeriod(movie);

        MovieExhibitionPeriod period = new MovieExhibitionPeriod();
        period.setPublicId(UUID.randomUUID().toString());
        period.setMovie(movie);
        period.setStartDate(request.getStartDate());
        period.setEndDate(request.getEndDate());
        period.setNote(request.getNote());
        MovieExhibitionPeriod saved = periodRepository.save(period);

        movie.setReleaseDate(request.getStartDate());
        movie.setEndDate(request.getEndDate());
        movieRepository.save(movie);
        return toResponse(saved);
    }

    private void preserveCurrentPeriod(Movie movie) {
        if (movie.getReleaseDate() == null
                || periodRepository.existsByMovieIdAndStartDateAndEndDateAndDeletedAtIsNull(
                        movie.getId(), movie.getReleaseDate(), movie.getEndDate())) {
            return;
        }
        MovieExhibitionPeriod previous = new MovieExhibitionPeriod();
        previous.setPublicId(UUID.randomUUID().toString());
        previous.setMovie(movie);
        previous.setStartDate(movie.getReleaseDate());
        previous.setEndDate(movie.getEndDate());
        previous.setNote("Đợt khai thác được lưu lại trước khi lập đợt mới.");
        periodRepository.save(previous);
    }

    private Movie findMovie(String publicId) {
        return movieRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Không tìm thấy phim."));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng chọn ngày bắt đầu khai thác.");
        }
        if (!startDate.isAfter(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ngày bắt đầu của đợt khai thác mới phải sau hôm nay.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ngày kết thúc khai thác không được trước ngày bắt đầu.");
        }
    }

    private MovieExhibitionPeriodResponse toResponse(MovieExhibitionPeriod period) {
        LocalDate today = LocalDate.now(clock);
        String state;
        if (period.getStartDate().isAfter(today)) {
            state = "UPCOMING";
        } else if (period.getEndDate() != null && period.getEndDate().isBefore(today)) {
            state = "ENDED";
        } else {
            state = "ACTIVE";
        }
        return new MovieExhibitionPeriodResponse(
                period.getPublicId(), period.getStartDate(), period.getEndDate(), state,
                period.getNote(), period.getCreatedAt());
    }
}
