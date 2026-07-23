package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.tmdb.dto.TmdbCollectionDiffDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbFieldDiffDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImageDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieReviewResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbPersonDto;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieTranslation;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.CreditRoleType;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieReadinessDto;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieTranslationRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.service.MovieHealthFacts;
import com.lorafilm.movie.movie.service.MovieLifecyclePolicy;
import com.lorafilm.movie.movie.service.MovieReadinessEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
public class TmdbMovieReviewService {

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final MovieProductionCompanyRepository movieProductionCompanyRepository;
    private final MovieTranslationRepository movieTranslationRepository;
    private final TmdbProviderMovieService providerMovieService;
    private final TmdbMovieMapper movieMapper;
    private final MovieReadinessEvaluator readinessEvaluator;
    private final MovieLifecyclePolicy lifecyclePolicy;

    public TmdbMovieReviewService(
            MovieRepository movieRepository,
            MovieGenreRepository movieGenreRepository,
            MovieVersionRepository movieVersionRepository,
            MovieMediaRepository movieMediaRepository,
            MovieCreditRepository movieCreditRepository,
            MovieProductionCompanyRepository movieProductionCompanyRepository,
            MovieTranslationRepository movieTranslationRepository,
            TmdbProviderMovieService providerMovieService,
            TmdbMovieMapper movieMapper,
            MovieReadinessEvaluator readinessEvaluator,
            MovieLifecyclePolicy lifecyclePolicy) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieCreditRepository = movieCreditRepository;
        this.movieProductionCompanyRepository = movieProductionCompanyRepository;
        this.movieTranslationRepository = movieTranslationRepository;
        this.providerMovieService = providerMovieService;
        this.movieMapper = movieMapper;
        this.readinessEvaluator = readinessEvaluator;
        this.lifecyclePolicy = lifecyclePolicy;
    }

    @Transactional(readOnly = true)
    public TmdbMovieReviewResponse getReview(String moviePublicId) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
        if (movie.getTmdbId() == null) {
            throw new BusinessException(ErrorCode.TMDB_MOVIE_REVIEW_NOT_APPLICABLE);
        }

        TmdbMovieWrapperDto provider = providerMovieService.fetchMovie(movie.getTmdbId());
        MovieReadinessDto readiness = readinessEvaluator.evaluate(MovieHealthFacts.from(
                movie,
                !movieGenreRepository.findByMovieId(movie.getId()).isEmpty(),
                movieVersionRepository.existsActiveVersion(movie.getId()),
                movieMediaRepository.existsPrimaryPoster(movie.getId())));

        List<String> approvalBlockers = new ArrayList<>();
        readiness.getBlockers().forEach(issue -> approvalBlockers.add(issue.getMessage()));
        approvalBlockers.addAll(lifecyclePolicy.getTransitionViolations(movie, MovieStatus.UPCOMING));
        boolean canApprove = movie.getStatus() == MovieStatus.DRAFT
                && readiness.getHealthStatus() != MovieHealthStatus.BLOCKED
                && approvalBlockers.isEmpty();

        List<TmdbFieldDiffDto> scalarDiffs = scalarDiffs(movie, provider);
        List<TmdbCollectionDiffDto> collectionDiffs = collectionDiffs(movie, provider);
        boolean changed = scalarDiffs.stream().anyMatch(TmdbFieldDiffDto::changed)
                || collectionDiffs.stream().anyMatch(TmdbCollectionDiffDto::changed);

        return new TmdbMovieReviewResponse(
                "TMDB",
                movie.getTmdbId(),
                reviewStatus(movie.getStatus()),
                canApprove,
                List.copyOf(approvalBlockers),
                readiness,
                movie.getTmdbLastUpdated(),
                provider.getLastUpdated(),
                changed,
                scalarDiffs,
                collectionDiffs);
    }

    private List<TmdbFieldDiffDto> scalarDiffs(Movie movie, TmdbMovieWrapperDto provider) {
        int providerDuration = provider.getMovie().getRuntimeMinutes() != null
                && provider.getMovie().getRuntimeMinutes() > 0
                ? provider.getMovie().getRuntimeMinutes()
                : 1;
        AgeRating providerAgeRating = Boolean.TRUE.equals(provider.getMovie().getAdult()) ? AgeRating.T18 : AgeRating.P;

        return List.of(
                field("title", "Tên phim", movie.getTitle(), movieMapper.extractTitle(provider)),
                field("originalTitle", "Tên gốc", movie.getOriginalTitle(), provider.getMovie().getOriginalTitle()),
                field("synopsis", "Mô tả", movie.getSynopsis(), movieMapper.extractOverview(provider)),
                field("durationMinutes", "Thời lượng", movie.getDurationMinutes(), providerDuration),
                field("ageRating", "Phân loại tuổi", movie.getAgeRating(), providerAgeRating),
                field("releaseDate", "Ngày khởi chiếu", movie.getReleaseDate(), movieMapper.extractReleaseDate(provider)),
                field("country", "Quốc gia", movie.getCountry(), movieMapper.extractCountry(provider)));
    }

    private List<TmdbCollectionDiffDto> collectionDiffs(Movie movie, TmdbMovieWrapperDto provider) {
        List<MovieMedia> media = movieMediaRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<MovieCredit> credits = movieCreditRepository.findByMovieIdAndDeletedAtIsNullOrderByDisplayOrderAsc(movie.getId());
        List<MovieProductionCompany> companies = movieProductionCompanyRepository.findByMovieId(movie.getId());
        List<MovieTranslation> translations = movieTranslationRepository.findByMovieId(movie.getId());

        List<String> currentGenres = movieGenreRepository.findByMovieId(movie.getId()).stream()
                .map(link -> clean(link.getGenre().getName())).toList();
        List<String> providerGenres = values(provider.getGenres(), genre -> clean(genre.getName()));

        List<String> currentMedia = media.stream().map(item -> String.join(" | ",
                item.getMediaType().name(),
                Boolean.TRUE.equals(item.getIsPrimary()) ? "PRIMARY" : "SECONDARY",
                clean(item.getUrl()))).toList();
        List<String> providerMedia = providerMedia(provider);

        List<String> currentCredits = credits.stream().map(this::currentCredit).toList();
        List<String> providerCredits = providerCredits(provider);

        List<String> currentCompanies = companies.stream().map(item -> String.join(" | ",
                item.getRole().name(),
                clean(item.getProductionCompany().getName()),
                clean(item.getProductionCompany().getCountry()))).toList();
        List<String> providerCompanies = values(provider.getProductionCompanies(), item -> String.join(" | ",
                "PRODUCTION", clean(item.getName()), clean(item.getOriginCountry())));

        List<String> currentTranslations = translations.stream().map(item -> String.join(" | ",
                clean(item.getLocale()), clean(item.getTitle()), clean(item.getSynopsis()))).toList();
        List<String> providerTranslations = providerTranslations(provider);

        return List.of(
                collection("genres", "Thể loại", currentGenres, providerGenres),
                collection("media", "Hình ảnh và trailer", currentMedia, providerMedia),
                collection("credits", "Đội ngũ", currentCredits, providerCredits),
                collection("companies", "Hãng sản xuất", currentCompanies, providerCompanies),
                collection("translations", "Bản dịch", currentTranslations, providerTranslations));
    }

    private List<String> providerMedia(TmdbMovieWrapperDto provider) {
        List<String> result = new ArrayList<>();
        if (provider.getVideos() != null && provider.getVideos().getPrimaryTrailer() != null
                && provider.getVideos().getPrimaryTrailer().getUrl() != null) {
            result.add("TRAILER | PRIMARY | " + clean(provider.getVideos().getPrimaryTrailer().getUrl()));
        }
        if (provider.getMedia() != null) {
            String primaryPoster = provider.getMedia().getPrimaryPoster() == null
                    ? null : provider.getMedia().getPrimaryPoster().getUrl();
            String primaryBackdrop = provider.getMedia().getPrimaryBackdrop() == null
                    ? null : provider.getMedia().getPrimaryBackdrop().getUrl();
            addImages(result, "POSTER", provider.getMedia().getPosters(), primaryPoster);
            addImages(result, "BANNER", provider.getMedia().getBackdrops(), primaryBackdrop);
        }
        return sortedDistinct(result);
    }

    private List<String> providerTranslations(TmdbMovieWrapperDto provider) {
        List<String> translations = new ArrayList<>(values(provider.getTranslations(), item -> String.join(" | ",
                providerLocale(item.getLocale(), item.getLanguageCode()),
                clean(item.getTitle() == null || item.getTitle().isBlank() ? movieMapper.extractTitle(provider) : item.getTitle()),
                clean(item.getOverview() == null || item.getOverview().isBlank() ? movieMapper.extractOverview(provider) : item.getOverview()))));
        boolean hasVietnamese = provider.getTranslations() != null && provider.getTranslations().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> clean(item.getLanguageCode()).toLowerCase(Locale.ROOT).contains("vi")
                        || clean(item.getLocale()).toLowerCase(Locale.ROOT).contains("vi"));
        if (!hasVietnamese) {
            translations.add(String.join(" | ", "vi-VN",
                    clean(movieMapper.extractTitle(provider)), clean(movieMapper.extractOverview(provider))));
        }
        return sortedDistinct(translations);
    }

    private void addImages(List<String> target, String type, List<TmdbImageDto> images, String primaryUrl) {
        if (images == null) return;
        images.stream().filter(Objects::nonNull).filter(image -> image.getUrl() != null)
                .forEach(image -> target.add(String.join(" | ", type,
                        Objects.equals(image.getUrl(), primaryUrl) ? "PRIMARY" : "SECONDARY",
                        clean(image.getUrl()))));
    }

    private String currentCredit(MovieCredit credit) {
        String identity = credit.getPerson().getTmdbPersonId() != null
                ? credit.getPerson().getTmdbPersonId().toString()
                : clean(credit.getPerson().getFullName());
        return String.join(" | ", credit.getRoleType().name(), identity, clean(credit.getCharacterName()));
    }

    private List<String> providerCredits(TmdbMovieWrapperDto provider) {
        if (provider.getCredits() == null) return List.of();
        List<String> values = new ArrayList<>();
        addPeople(values, CreditRoleType.DIRECTOR, provider.getCredits().getDirectors());
        addPeople(values, CreditRoleType.MAIN_ACTOR, provider.getCredits().getMainCast());
        addPeople(values, CreditRoleType.SUPPORTING_ACTOR, provider.getCredits().getSupportingCast());
        addPeople(values, CreditRoleType.WRITER, provider.getCredits().getWriters());
        addPeople(values, CreditRoleType.PRODUCER, provider.getCredits().getProducers());
        return sortedDistinct(values);
    }

    private void addPeople(List<String> target, CreditRoleType role, List<TmdbPersonDto> people) {
        if (people == null) return;
        people.stream().filter(Objects::nonNull).forEach(person -> {
            String identity = person.getTmdbPersonId() != null
                    ? person.getTmdbPersonId().toString()
                    : clean(person.getName());
            target.add(String.join(" | ", role.name(), identity, clean(person.getCharacter())));
        });
    }

    private TmdbFieldDiffDto field(String field, String label, Object current, Object provider) {
        String currentValue = current == null ? null : clean(String.valueOf(current));
        String providerValue = provider == null ? null : clean(String.valueOf(provider));
        return new TmdbFieldDiffDto(field, label, currentValue, providerValue, !Objects.equals(currentValue, providerValue));
    }

    private TmdbCollectionDiffDto collection(String field, String label, Collection<String> current, Collection<String> provider) {
        List<String> currentValues = sortedDistinct(current);
        List<String> providerValues = sortedDistinct(provider);
        Set<String> currentSet = new LinkedHashSet<>(currentValues);
        Set<String> providerSet = new LinkedHashSet<>(providerValues);
        List<String> added = providerValues.stream().filter(value -> !currentSet.contains(value)).toList();
        List<String> removed = currentValues.stream().filter(value -> !providerSet.contains(value)).toList();
        return new TmdbCollectionDiffDto(field, label, currentValues, providerValues, added, removed,
                !added.isEmpty() || !removed.isEmpty());
    }

    private <T> List<String> values(List<T> source, Function<T, String> mapper) {
        if (source == null) return List.of();
        return source.stream().filter(Objects::nonNull).map(mapper).toList();
    }

    private List<String> sortedDistinct(Collection<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                .distinct().sorted(Comparator.naturalOrder()).toList();
    }

    private String providerLocale(String locale, String languageCode) {
        return clean(locale == null || locale.isBlank() ? languageCode : locale);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String reviewStatus(MovieStatus status) {
        if (status == MovieStatus.DRAFT) return "PENDING";
        if (status == MovieStatus.INACTIVE) return "INACTIVE";
        return "ACTIVATED";
    }
}
