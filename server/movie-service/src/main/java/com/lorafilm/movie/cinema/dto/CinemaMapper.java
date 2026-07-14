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

    public CinemaMediaResponse toMediaResponse(com.lorafilm.movie.cinema.domain.entity.CinemaMedia media) {
        if (media == null) {
            return null;
        }
        CinemaMediaResponse response = new CinemaMediaResponse();
        response.setPublicId(media.getPublicId());
        response.setMediaType(media.getMediaType());
        response.setUrl(media.getUrl());
        response.setTitle(media.getTitle());
        response.setDisplayOrder(media.getDisplayOrder());
        response.setIsPrimary(media.getIsPrimary());
        response.setStatus(media.getStatus());
        return response;
    }

    public OperatingHourResponse toOperatingHourResponse(com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour hour) {
        if (hour == null) {
            return null;
        }
        OperatingHourResponse response = new OperatingHourResponse();
        response.setDayOfWeek(hour.getDayOfWeek());
        response.setOpenTime(Boolean.TRUE.equals(hour.getIsClosed()) ? null : hour.getOpenTime());
        response.setCloseTime(Boolean.TRUE.equals(hour.getIsClosed()) ? null : hour.getCloseTime());
        response.setIsClosed(hour.getIsClosed());
        return response;
    }

    public CinemaClosurePeriodResponse toClosurePeriodResponse(com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod period) {
        if (period == null) {
            return null;
        }
        CinemaClosurePeriodResponse response = new CinemaClosurePeriodResponse();
        response.setId(period.getId());
        if (period.getCinema() != null) {
            response.setCinemaPublicId(period.getCinema().getPublicId());
        }
        response.setStartTime(period.getStartTime());
        response.setEndTime(period.getEndTime());
        response.setReason(period.getReason());
        response.setStatus(period.getStatus());
        return response;
    }
}
