package com.lorafilm.movie.cinema.dto;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import org.springframework.stereotype.Component;

@Component
public class CinemaMapper {

    public CinemaDto toDto(Cinema cinema) {
        if (cinema == null) {
            return null;
        }
        CinemaDto dto = new CinemaDto();
        dto.setPublicId(cinema.getPublicId());
        dto.setName(cinema.getName());
        dto.setSlug(cinema.getSlug());
        dto.setCity(cinema.getCity());
        dto.setDistrict(cinema.getDistrict());
        dto.setAddress(cinema.getAddress());
        dto.setHotline(cinema.getHotline());
        dto.setLatitude(cinema.getLatitude());
        dto.setLongitude(cinema.getLongitude());
        dto.setTimezone(cinema.getTimezone());
        return dto;
    }

    public CinemaResponse toResponse(Cinema cinema) {
        if (cinema == null) {
            return null;
        }
        CinemaResponse response = new CinemaResponse();
        response.setPublicId(cinema.getPublicId());
        response.setName(cinema.getName());
        response.setSlug(cinema.getSlug());
        response.setCity(cinema.getCity());
        response.setDistrict(cinema.getDistrict());
        response.setAddress(cinema.getAddress());
        response.setHotline(cinema.getHotline());
        response.setLatitude(cinema.getLatitude());
        response.setLongitude(cinema.getLongitude());
        response.setTimezone(cinema.getTimezone());
        response.setDescription(cinema.getDescription());
        response.setStatus(cinema.getStatus());
        response.setOpenedDate(cinema.getOpenedDate());
        response.setClosedDate(cinema.getClosedDate());
        return response;
    }
}
