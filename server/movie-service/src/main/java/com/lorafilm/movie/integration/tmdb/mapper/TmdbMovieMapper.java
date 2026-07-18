package com.lorafilm.movie.integration.tmdb.mapper;

import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TmdbMovieMapper {

    @Mapping(target = "id", ignore = true) // Database auto-generated ID
    @Mapping(target = "tmdbId", source = "wrapper.tmdbId")
    @Mapping(target = "tmdbLastUpdated", source = "wrapper.lastUpdated")
    @Mapping(target = "publicId", expression = "java(generatePublicId())")
    @Mapping(target = "title", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getTitle() != null ? wrapper.getMovie().getTitle() : \"Unknown Title\")")
    @Mapping(target = "originalTitle", source = "wrapper.movie.originalTitle")
    @Mapping(target = "durationMinutes", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getRuntime() != null && wrapper.getMovie().getRuntime() > 0 ? wrapper.getMovie().getRuntime() : 1)")
    @Mapping(target = "synopsis", source = "wrapper.movie.overview")
    @Mapping(target = "status", expression = "java(getDefaultStatus())")
    @Mapping(target = "ageRating", expression = "java(wrapper.getMovie() != null && Boolean.TRUE.equals(wrapper.getMovie().getAdult()) ? com.lorafilm.movie.movie.domain.enums.AgeRating.T18 : com.lorafilm.movie.movie.domain.enums.AgeRating.P)")
    @Mapping(target = "releaseDate", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getReleaseDate() != null && !wrapper.getMovie().getReleaseDate().isEmpty() ? java.time.LocalDate.parse(wrapper.getMovie().getReleaseDate()) : java.time.LocalDate.now())")
    @Mapping(target = "slug", expression = "java(generateSlug(wrapper.getMovie() != null ? wrapper.getMovie().getTitle() : null))")
    @Mapping(target = "activeSlug", expression = "java(generateSlug(wrapper.getMovie() != null ? wrapper.getMovie().getTitle() : null))")
    Movie toEntity(TmdbMovieWrapperDto wrapper);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tmdbId", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "tmdbLastUpdated", source = "wrapper.lastUpdated")
    @Mapping(target = "title", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getTitle() != null ? wrapper.getMovie().getTitle() : entity.getTitle())")
    @Mapping(target = "originalTitle", source = "wrapper.movie.originalTitle")
    @Mapping(target = "durationMinutes", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getRuntime() != null && wrapper.getMovie().getRuntime() > 0 ? wrapper.getMovie().getRuntime() : entity.getDurationMinutes())")
    @Mapping(target = "synopsis", source = "wrapper.movie.overview")
    @Mapping(target = "releaseDate", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getReleaseDate() != null && !wrapper.getMovie().getReleaseDate().isEmpty() ? java.time.LocalDate.parse(wrapper.getMovie().getReleaseDate()) : entity.getReleaseDate())")
    void updateEntityFromDto(TmdbMovieWrapperDto wrapper, @MappingTarget Movie entity);

    default String generatePublicId() {
        return UUID.randomUUID().toString();
    }

    default MovieStatus getDefaultStatus() {
        return MovieStatus.DRAFT;
    }

    default String generateSlug(String title) {
        if (title == null) return UUID.randomUUID().toString();
        return title.toLowerCase().replaceAll("[^a-z0-9\\\\s-]", "").replaceAll("\\s+", "-");
    }
}
