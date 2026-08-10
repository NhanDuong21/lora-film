package com.lorafilm.movie.integration.tmdb.mapper;

import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbTranslationDto;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
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
        imports = {MovieStatus.class, AgeRating.class, UUID.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TmdbMovieMapper {

    @Mapping(target = "id", ignore = true) // Database auto-generated ID
    @Mapping(target = "tmdbId", source = "wrapper.tmdbId")
    @Mapping(target = "tmdbLastUpdated", source = "wrapper.lastUpdated")
    @Mapping(target = "publicId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "title", expression = "java(extractTitle(wrapper))")
    @Mapping(target = "originalTitle", source = "wrapper.movie.originalTitle")
    @Mapping(target = "durationMinutes", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getRuntimeMinutes() != null && wrapper.getMovie().getRuntimeMinutes() > 0 ? wrapper.getMovie().getRuntimeMinutes() : 1)")
    @Mapping(target = "synopsis", expression = "java(extractOverview(wrapper))")
    @Mapping(target = "country", expression = "java(extractCountry(wrapper))")
    @Mapping(target = "status", expression = "java(MovieStatus.DRAFT)")
    @Mapping(target = "ageRating", expression = "java(wrapper.getMovie() != null && Boolean.TRUE.equals(wrapper.getMovie().getAdult()) ? AgeRating.T18 : AgeRating.P)")
    @Mapping(target = "originalReleaseDate", expression = "java(extractReleaseDate(wrapper))")
    @Mapping(target = "releaseDate", expression = "java(extractInitialExhibitionDate(wrapper))")
    @Mapping(target = "slug", expression = "java(generateMovieSlug(extractTitle(wrapper), wrapper.getTmdbId()))")
    @Mapping(target = "activeSlug", expression = "java(generateMovieSlug(extractTitle(wrapper), wrapper.getTmdbId()))")
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
    @Mapping(target = "originalReleaseDate", expression = "java(wrapper.getMovie() != null && wrapper.getMovie().getReleaseDate() != null && !wrapper.getMovie().getReleaseDate().isEmpty() ? parseReleaseDate(wrapper.getMovie().getReleaseDate(), entity.getOriginalReleaseDate()) : entity.getOriginalReleaseDate())")
    @Mapping(target = "releaseDate", ignore = true)
    void updateEntityFromDto(TmdbMovieWrapperDto wrapper, @MappingTarget Movie entity);

    @Named("generatePublicId")
    default String generatePublicId() {
        return UUID.randomUUID().toString();
    }

    @Named("getDefaultStatus")
    default MovieStatus getDefaultStatus() {
        return MovieStatus.DRAFT;
    }

    @Named("generateMovieSlug")
    default String generateMovieSlug(String title, Long tmdbId) {
        String baseSlug = generateSlug(title);
        if (tmdbId != null) {
            return baseSlug + "-" + tmdbId;
        }
        return baseSlug;
    }

    @Named("generateSlug")
    default String generateSlug(String title) {
        if (title == null || title.isBlank()) return "movie-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Loại bỏ dấu tiếng Việt
        String temp = Normalizer.normalize(title, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(temp).replaceAll("");
        slug = slug.replaceAll("đ", "d").replaceAll("Đ", "D");
        
        // Thay khoảng trắng thành dấu gạch ngang, sau đó loại bỏ các ký tự đặc biệt
        slug = slug.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "").replaceAll("-+", "-");
        if (slug.isBlank() || "-".equals(slug)) {
            return "movie-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

    @Named("extractReleaseDate")
    default java.time.LocalDate extractReleaseDate(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null || wrapper.getMovie() == null) return null;
        return parseReleaseDate(wrapper.getMovie().getReleaseDate(), null);
    }

    /**
     * Phim chưa phát hành có thể dùng ngày từ TMDB làm kế hoạch khai thác ban đầu.
     * Phim cũ phải để admin chủ động lập một đợt khai thác mới, tránh xem ngày
     * phát hành gốc là ngày phim từng được khai thác tại cụm rạp của hệ thống.
     */
    default java.time.LocalDate extractInitialExhibitionDate(TmdbMovieWrapperDto wrapper) {
        java.time.LocalDate originalDate = extractReleaseDate(wrapper);
        if (originalDate == null) return null;
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        return originalDate.isBefore(today) ? null : originalDate;
    }

    @Named("parseReleaseDate")
    default java.time.LocalDate parseReleaseDate(String releaseDateStr, java.time.LocalDate fallback) {
        if (releaseDateStr == null || releaseDateStr.isBlank()) {
            return fallback;
        }
        String trimmed = releaseDateStr.trim();
        try {
            return java.time.LocalDate.parse(trimmed);
        } catch (Exception e) {
            if (trimmed.matches("^\\d{4}$")) {
                try {
                    return java.time.LocalDate.of(Integer.parseInt(trimmed), 1, 1);
                } catch (Exception ignored) {}
            }
            return fallback;
        }
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

        // 1. Prioritize Vietnamese Overview from translations
        if (wrapper.getTranslations() != null) {
            for (TmdbTranslationDto t : wrapper.getTranslations()) {
                if (t != null && t.getOverview() != null && !t.getOverview().trim().isEmpty()) {
                    String lang = t.getLanguageCode() != null ? t.getLanguageCode().toLowerCase() : "";
                    String loc = t.getLocale() != null ? t.getLocale().toLowerCase() : "";
                    String country = t.getCountryCode() != null ? t.getCountryCode().toLowerCase() : "";
                    
                    if (lang.contains("vi") || loc.contains("vi") || "vn".equals(country)) {
                        return t.getOverview().trim();
                    }
                }
            }
        }

        // 2. Fallback to main movie overview
        if (wrapper.getMovie() != null && wrapper.getMovie().getOverview() != null && !wrapper.getMovie().getOverview().trim().isEmpty()) {
            return wrapper.getMovie().getOverview().trim();
        }
        
        // 3. Fallback to any non-empty overview in translations (e.g., English / Original)
        if (wrapper.getTranslations() != null) {
            for (TmdbTranslationDto t : wrapper.getTranslations()) {
                if (t != null && t.getOverview() != null && !t.getOverview().trim().isEmpty()) {
                    return t.getOverview().trim();
                }
            }
        }
        
        return null;
    }

    @Named("extractTitle")
    default String extractTitle(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) return "Unknown Title";
        
        // 1. Prioritize Vietnamese Title from translations
        if (wrapper.getTranslations() != null) {
            for (TmdbTranslationDto t : wrapper.getTranslations()) {
                if (t != null && t.getTitle() != null && !t.getTitle().trim().isEmpty()) {
                    String lang = t.getLanguageCode() != null ? t.getLanguageCode().toLowerCase() : "";
                    String loc = t.getLocale() != null ? t.getLocale().toLowerCase() : "";
                    String country = t.getCountryCode() != null ? t.getCountryCode().toLowerCase() : "";
                    
                    if (lang.contains("vi") || loc.contains("vi") || "vn".equals(country)) {
                        return t.getTitle().trim();
                    }
                }
            }
        }

        // 2. If main movie title differs from originalTitle, Node API fetched a localized Vietnamese title
        if (wrapper.getMovie() != null && wrapper.getMovie().getTitle() != null && !wrapper.getMovie().getTitle().trim().isEmpty()) {
            String title = wrapper.getMovie().getTitle().trim();
            String originalTitle = wrapper.getMovie().getOriginalTitle() != null ? wrapper.getMovie().getOriginalTitle().trim() : "";
            
            if (!title.equalsIgnoreCase(originalTitle)) {
                return title; // Vietnamese localized title
            }
        }
        
        // 3. Fallback strictly to Original Title (Bản gốc)
        if (wrapper.getMovie() != null && wrapper.getMovie().getOriginalTitle() != null && !wrapper.getMovie().getOriginalTitle().trim().isEmpty()) {
            return wrapper.getMovie().getOriginalTitle().trim();
        }

        if (wrapper.getMovie() != null && wrapper.getMovie().getTitle() != null && !wrapper.getMovie().getTitle().trim().isEmpty()) {
            return wrapper.getMovie().getTitle().trim();
        }
        
        return "Unknown Title";
    }
}
