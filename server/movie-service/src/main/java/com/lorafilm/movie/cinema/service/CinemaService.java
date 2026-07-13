package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CreateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaRequest;
import com.lorafilm.movie.cinema.dto.CinemaResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaMediaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaMediaRequest;
import com.lorafilm.movie.cinema.dto.CinemaMediaResponse;
import com.lorafilm.movie.cinema.dto.OperatingHourUpdateRequest;
import com.lorafilm.movie.cinema.dto.OperatingHourResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;
import com.lorafilm.movie.cinema.dto.CinemaClosurePeriodResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import java.util.List;

public interface CinemaService {
    PageResponse<CinemaDto> getCinemas(String city, String district, String keyword, int page, int size);
    CinemaDetailDto getCinemaBySlug(String slug);
    CinemaDetailDto getCinemaByIdentifier(String identifier);
    CinemaResponse createCinema(CreateCinemaRequest request);
    CinemaResponse updateCinema(String publicId, UpdateCinemaRequest request);
    CinemaResponse updateCinemaStatus(String publicId, com.lorafilm.movie.cinema.domain.enums.CinemaStatus targetStatus);

    List<CinemaDetailDto.CinemaMediaDto> getCinemaMedia(String cinemaPublicId);
    List<CinemaDetailDto.OperatingHourDto> getCinemaOperatingHours(String cinemaPublicId);
    CinemaMediaResponse addCinemaMedia(String cinemaPublicId, CreateCinemaMediaRequest request);
    CinemaMediaResponse updateCinemaMedia(String mediaPublicId, UpdateCinemaMediaRequest request);
    List<OperatingHourResponse> updateOperatingHours(String cinemaPublicId, List<OperatingHourUpdateRequest> requests);
    CinemaClosurePeriodResponse createClosurePeriod(String cinemaPublicId, CreateCinemaClosurePeriodRequest request);
    CinemaClosurePeriodResponse cancelClosurePeriod(Long closurePeriodId);
}
