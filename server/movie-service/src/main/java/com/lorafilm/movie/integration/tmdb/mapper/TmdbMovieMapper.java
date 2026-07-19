package com.lorafilm.movie.integration.tmdb.mapper;

import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbTranslationDto;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TmdbMovieMapper {

    @Mapping(target = "id", ignore = true) // Database auto-generated ID
    @Mapping(target = "tmdbId", source = "wrapper.tmdbId")
    @Mapping(target = "tmdbLastUpdated", source = "wrapper.lastUpdated")
    @Mapping(target = "publicId", expression = "java(generatePublicId())")
    @Mapping(target = "title", expression = "java(extractTitle(wrapper))")
    @Mapping(target = "originalTitle", source = "wrapper.movie.originalTitle")
    @Mapping(target = "durationMinutes", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getRuntimeMinutes() != null && wrapper.getMovie().getRuntimeMinutes() > 0 ? wrapper.getMovie().getRuntimeMinutes() : 1)")
    @Mapping(target = "synopsis", expression = "java(extractOverview(wrapper))")
    @Mapping(target = "country", expression = "java(extractCountry(wrapper))")
    @Mapping(target = "status", expression = "java(getDefaultStatus())")
    @Mapping(target = "ageRating", expression = "java(wrapper.getMovie() != null && Boolean.TRUE.equals(wrapper.getMovie().getAdult()) ? com.lorafilm.movie.movie.domain.enums.AgeRating.T18 : com.lorafilm.movie.movie.domain.enums.AgeRating.P)")
    @Mapping(target = "releaseDate", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getReleaseDate() != null && !wrapper.getMovie().getReleaseDate().isEmpty() ? java.time.LocalDate.parse(wrapper.getMovie().getReleaseDate()) : java.time.LocalDate.now())")
    @Mapping(target = "slug", expression = "java(generateSlug(extractTitle(wrapper)))")
    @Mapping(target = "activeSlug", expression = "java(generateSlug(extractTitle(wrapper)))")
    Movie toEntity(TmdbMovieWrapperDto wrapper);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tmdbId", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "tmdbLastUpdated", source = "wrapper.lastUpdated")
    @Mapping(target = "title", expression = "java(extractTitle(wrapper) != null && !\"Unknown Title\".equals(extractTitle(wrapper)) ? extractTitle(wrapper) : entity.getTitle())")
    @Mapping(target = "originalTitle", source = "wrapper.movie.originalTitle")
    @Mapping(target = "durationMinutes", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getRuntimeMinutes() != null && wrapper.getMovie().getRuntimeMinutes() > 0 ? wrapper.getMovie().getRuntimeMinutes() : entity.getDurationMinutes())")
    @Mapping(target = "synopsis", expression = "java(extractOverview(wrapper))")
    @Mapping(target = "country", expression = "java(extractCountry(wrapper) != null ? extractCountry(wrapper) : entity.getCountry())")
    @Mapping(target = "releaseDate", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getReleaseDate() != null && !wrapper.getMovie().getReleaseDate().isEmpty() ? java.time.LocalDate.parse(wrapper.getMovie().getReleaseDate()) : entity.getReleaseDate())")
    void updateEntityFromDto(TmdbMovieWrapperDto wrapper, @MappingTarget Movie entity);

    @Named("generatePublicId")
    default String generatePublicId() {
        return UUID.randomUUID().toString();
    }

    @Named("getDefaultStatus")
    default MovieStatus getDefaultStatus() {
        return MovieStatus.DRAFT;
    }

    @Named("generateSlug")
    default String generateSlug(String title) {
        if (title == null) return UUID.randomUUID().toString();
        
        // Loại bỏ dấu tiếng Việt
        String temp = Normalizer.normalize(title, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(temp).replaceAll("");
        slug = slug.replaceAll("đ", "d").replaceAll("Đ", "D");
        
        // Thay khoảng trắng thành dấu gạch ngang, sau đó loại bỏ các ký tự đặc biệt
        return slug.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");
    }

    @Named("extractCountry")
    default String extractCountry(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null || wrapper.getMovie() == null) return null;
        if (wrapper.getMovie().getCountries() != null && !wrapper.getMovie().getCountries().isEmpty()) {
            return wrapper.getMovie().getCountries().get(0).getIsoCode();
        }
        return null;
    }

    @Named("extractOverview")
    default String extractOverview(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) return null;
        if (wrapper.getTranslations() != null) {
            for (TmdbTranslationDto t : wrapper.getTranslations()) {
                if (("vi".equalsIgnoreCase(t.getLanguageCode()) || "vi".equalsIgnoreCase(t.getLocale()) || "VN".equalsIgnoreCase(t.getCountryCode())) && t.getOverview() != null && !t.getOverview().trim().isEmpty()) {
                    return t.getOverview();
                }
            }
        }
        return wrapper.getMovie() != null ? wrapper.getMovie().getOverview() : null;
    }

    @Named("extractTitle")
    default String extractTitle(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) return "Unknown Title";
        if (wrapper.getTranslations() != null) {
            for (TmdbTranslationDto t : wrapper.getTranslations()) {
                if (("vi".equalsIgnoreCase(t.getLanguageCode()) || "vi".equalsIgnoreCase(t.getLocale()) || "VN".equalsIgnoreCase(t.getCountryCode())) && t.getTitle() != null && !t.getTitle().trim().isEmpty()) {
                    return t.getTitle();
                }
            }
        }
        return (wrapper.getMovie() != null && wrapper.getMovie().getTitle() != null) ? wrapper.getMovie().getTitle() : "Unknown Title";
    }
}
