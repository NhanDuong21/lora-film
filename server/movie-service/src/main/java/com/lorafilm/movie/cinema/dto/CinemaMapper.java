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
}
