package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.cinema.util.SlugUtils;
import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.Person;
import com.lorafilm.movie.movie.domain.enums.CreditRoleType;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.people.PublicPersonCardResponse;
import com.lorafilm.movie.movie.dto.people.PublicPersonDetailResponse;
import com.lorafilm.movie.movie.dto.people.PublicPersonMovieResponse;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PublicPersonService {

    private static final Pattern PUBLIC_ID_SUFFIX = Pattern.compile(
            "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$");
    private static final Set<MovieStatus> PUBLIC_DETAIL_STATUSES =
            EnumSet.of(MovieStatus.NOW_SHOWING, MovieStatus.UPCOMING, MovieStatus.ENDED);

    private final PersonRepository personRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final Clock clock;

    public PublicPersonService(
            PersonRepository personRepository,
            MovieCreditRepository movieCreditRepository,
            MovieMediaRepository movieMediaRepository,
            Clock clock) {
        this.personRepository = personRepository;
        this.movieCreditRepository = movieCreditRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.clock = clock;
    }

    public PageResponse<PublicPersonCardResponse> getPeople(
            String roleValue,
            String query,
            String availabilityValue,
            String sortValue,
            int page,
            int size) {
        PublicRole role = PublicRole.parse(roleValue);
        Availability availability = Availability.parse(availabilityValue);
        SortMode sort = SortMode.parse(sortValue);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 40));
        LocalDate today = LocalDate.now(clock);

        Page<PersonRepository.PersonCatalogProjection> result = personRepository.findCatalogPeople(
                ActiveStatus.ACTIVE,
                role.creditRoles,
                availability.movieStatuses,
                today,
                query == null ? "" : query.trim(),
                sort.name(),
                PageRequest.of(safePage, safeSize));

        List<Long> personIds = result.getContent().stream()
                .map(PersonRepository.PersonCatalogProjection::getPersonId)
                .toList();
        if (personIds.isEmpty()) {
            return new PageResponse<>(List.of(), result.getNumber(), result.getSize(),
                    result.getTotalElements(), result.getTotalPages(), result.isLast());
        }

        Map<Long, Person> peopleById = personRepository.findByIdIn(personIds).stream()
                .collect(Collectors.toMap(Person::getId, person -> person));
        Map<Long, List<MovieCredit>> creditsByPerson = movieCreditRepository
                .findCatalogCreditsForPeople(personIds, role.creditRoles,
                        availability.movieStatuses, today)
                .stream()
                .collect(Collectors.groupingBy(
                        credit -> credit.getPerson().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, Long> popularityByPerson = result.getContent().stream()
                .collect(Collectors.toMap(
                        PersonRepository.PersonCatalogProjection::getPersonId,
                        item -> item.getCreditCount() == null ? 0L : item.getCreditCount()));

        List<PublicPersonCardResponse> cards = personIds.stream()
                .map(peopleById::get)
                .filter(java.util.Objects::nonNull)
                .map(person -> toCard(
                        person,
                        creditsByPerson.getOrDefault(person.getId(), List.of()),
                        role,
                        popularityByPerson.getOrDefault(person.getId(), 0L)))
                .toList();

        return new PageResponse<>(cards, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    public PublicPersonDetailResponse getPerson(String identifier) {
        Person person = findVisiblePerson(identifier);
        LocalDate today = LocalDate.now(clock);
        List<MovieCredit> credits = movieCreditRepository.findPublicCreditsForPerson(
                person.getId(), PUBLIC_DETAIL_STATUSES, today);
        if (credits.isEmpty()) {
            throw new BusinessException(ErrorCode.PERSON_NOT_FOUND,
                    "Nghệ sĩ này chưa có tác phẩm trong danh mục LoraFilm.", null);
        }

        Map<Long, String> posters = loadPosters(credits);
        List<PublicPersonMovieResponse> movies = aggregateMovies(credits, posters);
        List<String> roles = credits.stream()
                .map(MovieCredit::getRoleType)
                .map(PublicPersonService::roleLabel)
                .distinct()
                .toList();

        List<PublicPersonMovieResponse> available = movies.stream()
                .filter(movie -> "NOW_SHOWING".equals(movie.availability()))
                .toList();
        List<PublicPersonMovieResponse> upcoming = movies.stream()
                .filter(movie -> "UPCOMING".equals(movie.availability()))
                .toList();
        List<PublicPersonMovieResponse> other = movies.stream()
                .filter(movie -> "ENDED".equals(movie.availability()))
                .toList();

        return new PublicPersonDetailResponse(
                person.getPublicId(),
                slugFor(person),
                displayName(person),
                originalName(person),
                blankToNull(person.getProfileImageUrl()),
                cleanBiography(person.getBiography()),
                person.getBirthDate(),
                blankToNull(person.getNationality()),
                roles,
                available,
                upcoming,
                other);
    }

    public List<PublicPersonMovieResponse> getPersonMovies(String identifier, String availabilityValue) {
        PublicPersonDetailResponse detail = getPerson(identifier);
        Availability availability = Availability.parse(availabilityValue);
        return switch (availability) {
            case NOW_SHOWING -> detail.availableMovies();
            case UPCOMING -> detail.upcomingMovies();
            case ALL -> {
                List<PublicPersonMovieResponse> combined = new ArrayList<>();
                combined.addAll(detail.availableMovies());
                combined.addAll(detail.upcomingMovies());
                combined.addAll(detail.otherCredits());
                yield List.copyOf(combined);
            }
        };
    }

    private Person findVisiblePerson(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        Matcher matcher = PUBLIC_ID_SUFFIX.matcher(normalized);
        String publicId = matcher.find() ? matcher.group(1) : normalized;
        Person person = personRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PERSON_NOT_FOUND, "Không tìm thấy nghệ sĩ.", null));
        if (person.getStatus() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PERSON_NOT_FOUND, "Không tìm thấy nghệ sĩ.", null);
        }
        return person;
    }

    private PublicPersonCardResponse toCard(
            Person person,
            List<MovieCredit> credits,
            PublicRole role,
            long popularityScore) {
        List<String> knownFor = credits.stream()
                .map(MovieCredit::getMovie)
                .filter(java.util.Objects::nonNull)
                .map(Movie::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .distinct()
                .limit(3)
                .toList();
        String characterName = credits.stream()
                .map(MovieCredit::getCharacterName)
                .map(PublicPersonService::blankToNull)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        return new PublicPersonCardResponse(
                person.getPublicId(),
                slugFor(person),
                displayName(person),
                originalName(person),
                blankToNull(person.getProfileImageUrl()),
                List.of(role.label),
                knownFor,
                characterName,
                popularityScore);
    }

    private Map<Long, String> loadPosters(List<MovieCredit> credits) {
        List<Long> movieIds = credits.stream()
                .map(MovieCredit::getMovie)
                .filter(java.util.Objects::nonNull)
                .map(Movie::getId)
                .distinct()
                .toList();
        if (movieIds.isEmpty()) return Map.of();
        return movieMediaRepository
                .findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        movieIds, MovieMediaType.POSTER, ActiveStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        media -> media.getMovie().getId(),
                        media -> media.getUrl(),
                        (first, ignored) -> first));
    }

    private List<PublicPersonMovieResponse> aggregateMovies(
            List<MovieCredit> credits,
            Map<Long, String> posters) {
        Map<Long, MovieCreditAggregate> aggregates = new LinkedHashMap<>();
        for (MovieCredit credit : credits) {
            Movie movie = credit.getMovie();
            MovieCreditAggregate aggregate = aggregates.computeIfAbsent(
                    movie.getId(), ignored -> new MovieCreditAggregate(movie));
            aggregate.roles.add(roleLabel(credit.getRoleType()));
            String character = blankToNull(credit.getCharacterName());
            if (aggregate.characterName == null && character != null) {
                aggregate.characterName = character;
            }
        }
        return aggregates.values().stream()
                .map(item -> new PublicPersonMovieResponse(
                        item.movie.getPublicId(),
                        item.movie.getSlug(),
                        item.movie.getTitle(),
                        posters.get(item.movie.getId()),
                        item.movie.getReleaseDate(),
                        item.movie.getStatus().name(),
                        String.join(" · ", item.roles),
                        item.characterName))
                .sorted(Comparator
                        .comparingInt((PublicPersonMovieResponse movie) -> availabilityRank(movie.availability()))
                        .thenComparing(PublicPersonMovieResponse::releaseDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PublicPersonMovieResponse::title,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private static int availabilityRank(String status) {
        return switch (status) {
            case "NOW_SHOWING" -> 0;
            case "UPCOMING" -> 1;
            default -> 2;
        };
    }

    private static String slugFor(Person person) {
        String name = displayName(person);
        String base;
        try {
            base = SlugUtils.toSlug(name);
        } catch (IllegalArgumentException exception) {
            base = "nghe-si";
        }
        return base + "-" + person.getPublicId();
    }

    private static String displayName(Person person) {
        String stageName = blankToNull(person.getStageName());
        return stageName != null ? stageName : person.getFullName();
    }

    private static String originalName(Person person) {
        String fullName = blankToNull(person.getFullName());
        if (fullName == null || fullName.equalsIgnoreCase(displayName(person))) return null;
        return fullName;
    }

    private static String cleanBiography(String biography) {
        String value = blankToNull(biography);
        return value == null || "Không có thông tin".equalsIgnoreCase(value) ? null : value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String roleLabel(CreditRoleType role) {
        return switch (role) {
            case DIRECTOR -> "Đạo diễn";
            case MAIN_ACTOR, SUPPORTING_ACTOR, VOICE_ACTOR, GUEST -> "Diễn viên";
            case WRITER -> "Biên kịch";
            case PRODUCER -> "Nhà sản xuất";
        };
    }

    private enum PublicRole {
        ACTOR("Diễn viên", EnumSet.of(
                CreditRoleType.MAIN_ACTOR,
                CreditRoleType.SUPPORTING_ACTOR,
                CreditRoleType.VOICE_ACTOR,
                CreditRoleType.GUEST)),
        DIRECTOR("Đạo diễn", EnumSet.of(CreditRoleType.DIRECTOR));

        private final String label;
        private final Set<CreditRoleType> creditRoles;

        PublicRole(String label, Set<CreditRoleType> creditRoles) {
            this.label = label;
            this.creditRoles = creditRoles;
        }

        static PublicRole parse(String value) {
            try {
                return value == null ? ACTOR : valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Vai trò chỉ chấp nhận ACTOR hoặc DIRECTOR.", null);
            }
        }
    }

    private enum Availability {
        ALL(EnumSet.of(MovieStatus.NOW_SHOWING, MovieStatus.UPCOMING)),
        NOW_SHOWING(EnumSet.of(MovieStatus.NOW_SHOWING)),
        UPCOMING(EnumSet.of(MovieStatus.UPCOMING));

        private final Set<MovieStatus> movieStatuses;

        Availability(Set<MovieStatus> movieStatuses) {
            this.movieStatuses = movieStatuses;
        }

        static Availability parse(String value) {
            try {
                return value == null || value.isBlank()
                        ? ALL
                        : valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Tình trạng chỉ chấp nhận ALL, NOW_SHOWING hoặc UPCOMING.", null);
            }
        }
    }

    private enum SortMode {
        POPULAR,
        NAME_ASC,
        NEW;

        static SortMode parse(String value) {
            if (value == null || value.isBlank()) return POPULAR;
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if ("A_Z".equals(normalized) || "NAME".equals(normalized)) return NAME_ASC;
            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Sắp xếp chỉ chấp nhận POPULAR, NAME_ASC hoặc NEW.", null);
            }
        }
    }

    private static final class MovieCreditAggregate {
        private final Movie movie;
        private final Set<String> roles = new LinkedHashSet<>();
        private String characterName;

        private MovieCreditAggregate(Movie movie) {
            this.movie = movie;
        }
    }
}
