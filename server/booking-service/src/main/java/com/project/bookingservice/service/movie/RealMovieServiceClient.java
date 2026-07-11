package com.project.bookingservice.service.movie;

import com.project.bookingservice.dto.movie.SeatInfo;
import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Profile("!local & !test")
public class RealMovieServiceClient implements MovieServiceClient {

    private final RestTemplate restTemplate;
    private final String movieServiceUrl;

    public RealMovieServiceClient(RestTemplate restTemplate, @Value("${movie-service.url:http://movie-service}") String movieServiceUrl) {
        this.restTemplate = restTemplate;
        this.movieServiceUrl = movieServiceUrl;
    }

    @Override
    public ShowtimeInfo getShowtime(Long showtimeId) {
        try {
            return restTemplate.getForObject(movieServiceUrl + "/api/showtimes/" + showtimeId, ShowtimeInfo.class);
        } catch (ResourceAccessException e) {
            throw new BusinessException("MOVIE_SERVICE_UNAVAILABLE", "Movie service is down or timed out");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<SeatInfo> getSeats(List<Long> seatIds) {
        try {
            String url = movieServiceUrl + "/api/seats?ids=" + String.join(",", seatIds.stream().map(String::valueOf).toList());
            ResponseEntity<List<SeatInfo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<SeatInfo>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (ResourceAccessException e) {
            throw new BusinessException("MOVIE_SERVICE_UNAVAILABLE", "Movie service is down or timed out");
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean isSeatBooked(Long showtimeId, Long seatId) {
        try {
            Boolean isBooked = restTemplate.getForObject(movieServiceUrl + "/api/showtimes/" + showtimeId + "/seats/" + seatId + "/booked", Boolean.class);
            return Boolean.TRUE.equals(isBooked);
        } catch (ResourceAccessException e) {
            throw new BusinessException("MOVIE_SERVICE_UNAVAILABLE", "Movie service is down or timed out");
        } catch (Exception e) {
            return false;
        }
    }
}
