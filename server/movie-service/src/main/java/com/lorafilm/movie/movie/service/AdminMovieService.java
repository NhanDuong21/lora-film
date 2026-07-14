package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.repository.GenreRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminMovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMapper movieMapper;
    private final com.lorafilm.movie.showtime.repository.ShowtimeRepository showtimeRepository;
    private final com.lorafilm.movie.movie.repository.PersonRepository personRepository;
    private final com.lorafilm.movie.movie.repository.ProductionCompanyRepository productionCompanyRepository;
    private final com.lorafilm.movie.movie.repository.MovieCreditRepository movieCreditRepository;
    private final com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository movieProductionCompanyRepository;

    public AdminMovieService(MovieRepository movieRepository, GenreRepository genreRepository, MovieGenreRepository movieGenreRepository, MovieMapper movieMapper, com.lorafilm.movie.showtime.repository.ShowtimeRepository showtimeRepository,
                             com.lorafilm.movie.movie.repository.PersonRepository personRepository,
                             com.lorafilm.movie.movie.repository.ProductionCompanyRepository productionCompanyRepository,
                             com.lorafilm.movie.movie.repository.MovieCreditRepository movieCreditRepository,
                             com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository movieProductionCompanyRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMapper = movieMapper;
        this.showtimeRepository = showtimeRepository;
        this.personRepository = personRepository;
        this.productionCompanyRepository = productionCompanyRepository;
        this.movieCreditRepository = movieCreditRepository;
        this.movieProductionCompanyRepository = movieProductionCompanyRepository;
    }

    @Transactional
    public MovieDto createMovie(MovieRequest request) {
        validateMovieDates(request);
        
        String slug = generateUniqueSlug(request.getTitle());

        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle(request.getTitle());
        movie.setOriginalTitle(request.getOriginalTitle());
        movie.setSynopsis(request.getSynopsis());
        movie.setSlug(slug);
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(request.getAgeRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setCountry(request.getCountry());
        movie.setStatus(MovieStatus.DRAFT);
        
        Movie saved = movieRepository.save(movie);
        return mapToDto(saved);
    }

    @Transactional
    public MovieDto updateMovie(String publicId, MovieRequest request) {
        validateMovieDates(request);

        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));

        if (!movie.getTitle().equals(request.getTitle())) {
            movie.setSlug(generateUniqueSlug(request.getTitle()));
        }

        movie.setTitle(request.getTitle());
        movie.setOriginalTitle(request.getOriginalTitle());
        movie.setSynopsis(request.getSynopsis());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(request.getAgeRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setCountry(request.getCountry());
        
        if (request.getStatus() != null && request.getStatus() != movie.getStatus()) {
            validatePublishStatus(movie.getId(), request.getStatus());
            movie.setStatus(request.getStatus());
        }
        
        validateStatusTimeConstraints(movie.getStatus(), movie.getReleaseDate(), movie.getEndDate());

        Movie saved = movieRepository.save(movie);
        return mapToDto(saved);
    }

    @Transactional
    public void assignGenres(String moviePublicId, List<String> genreIds) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        if ((movie.getStatus() == MovieStatus.UPCOMING || movie.getStatus() == MovieStatus.NOW_SHOWING) 
            && (genreIds == null || genreIds.isEmpty())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Published movie must have at least one genre", null);
        }

        List<Genre> genres = genreRepository.findByPublicIdInAndDeletedAtIsNull(genreIds);
        if (genres.size() != (genreIds == null ? 0 : genreIds.size())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "One or more genres do not exist", null);
        }

        movieGenreRepository.deleteByMovieId(movie.getId());
        
        for (Genre genre : genres) {
            MovieGenre movieGenre = new MovieGenre();
            movieGenre.setMovie(movie);
            movieGenre.setGenre(genre);
            movieGenreRepository.save(movieGenre);
        }
    }

    @Transactional
    public void appendGenres(String moviePublicId, List<String> genreIds) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        if (genreIds == null || genreIds.isEmpty()) return;

        List<Genre> genres = genreRepository.findByPublicIdInAndDeletedAtIsNull(genreIds);
        if (genres.size() != genreIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "One or more genres do not exist", null);
        }
        
        for (Genre genre : genres) {
            if (!movieGenreRepository.existsByMovieIdAndGenreId(movie.getId(), genre.getId())) {
                MovieGenre movieGenre = new MovieGenre();
                movieGenre.setMovie(movie);
                movieGenre.setGenre(genre);
                movieGenreRepository.save(movieGenre);
            }
        }
    }

    @Transactional
    public void assignCredits(String moviePublicId, List<com.lorafilm.movie.movie.dto.MovieCreditRequest> requests) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        movieCreditRepository.deleteByMovieId(movie.getId());
        
        if (requests == null || requests.isEmpty()) return;
        
        for (com.lorafilm.movie.movie.dto.MovieCreditRequest req : requests) {
            com.lorafilm.movie.movie.domain.entity.Person person = personRepository.findByPublicIdAndDeletedAtIsNull(req.getPersonPublicId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Person not found", null));
            
            com.lorafilm.movie.movie.domain.entity.MovieCredit credit = new com.lorafilm.movie.movie.domain.entity.MovieCredit();
            credit.setMovie(movie);
            credit.setPerson(person);
            credit.setRoleType(req.getRoleType());
            credit.setCharacterName(req.getCharacterName());
            credit.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
            
            movieCreditRepository.save(credit);
        }
    }

    @Transactional
    public void appendCredits(String moviePublicId, List<com.lorafilm.movie.movie.dto.MovieCreditRequest> requests) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));

        if (requests == null || requests.isEmpty()) return;

        for (com.lorafilm.movie.movie.dto.MovieCreditRequest req : requests) {
            com.lorafilm.movie.movie.domain.entity.Person person = personRepository.findByPublicIdAndDeletedAtIsNull(req.getPersonPublicId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Person not found", null));

            if (movieCreditRepository.existsByMovieIdAndPersonIdAndRoleTypeAndDeletedAtIsNull(movie.getId(), person.getId(), req.getRoleType())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Person already has this role in the movie", null);
            }

            com.lorafilm.movie.movie.domain.entity.MovieCredit credit = new com.lorafilm.movie.movie.domain.entity.MovieCredit();
            credit.setMovie(movie);
            credit.setPerson(person);
            credit.setRoleType(req.getRoleType());
            credit.setCharacterName(req.getCharacterName());
            credit.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);

            movieCreditRepository.save(credit);
        }
    }

    @Transactional
    public void assignProductionCompanies(String moviePublicId, List<com.lorafilm.movie.movie.dto.MovieCompanyRequest> requests) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        movieProductionCompanyRepository.deleteByMovieId(movie.getId());
        
        if (requests == null || requests.isEmpty()) return;
        
        for (com.lorafilm.movie.movie.dto.MovieCompanyRequest req : requests) {
            com.lorafilm.movie.movie.domain.entity.ProductionCompany company = productionCompanyRepository.findByPublicIdAndDeletedAtIsNull(req.getCompanyPublicId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Production company not found", null));
            
            com.lorafilm.movie.movie.domain.entity.MovieProductionCompany mpc = new com.lorafilm.movie.movie.domain.entity.MovieProductionCompany();
            mpc.setMovie(movie);
            mpc.setProductionCompany(company);
            mpc.setRole(req.getRole());
            
            movieProductionCompanyRepository.save(mpc);
        }
    }

    @Transactional
    public void appendProductionCompanies(String moviePublicId, List<com.lorafilm.movie.movie.dto.MovieCompanyRequest> requests) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        if (requests == null || requests.isEmpty()) return;
        
        for (com.lorafilm.movie.movie.dto.MovieCompanyRequest req : requests) {
            com.lorafilm.movie.movie.domain.entity.ProductionCompany company = productionCompanyRepository.findByPublicIdAndDeletedAtIsNull(req.getCompanyPublicId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Production company not found", null));
            
            if (movieProductionCompanyRepository.existsByMovieIdAndProductionCompanyIdAndRole(movie.getId(), company.getId(), req.getRole())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Company already has this role in the movie", null);
            }

            com.lorafilm.movie.movie.domain.entity.MovieProductionCompany mpc = new com.lorafilm.movie.movie.domain.entity.MovieProductionCompany();
            mpc.setMovie(movie);
            mpc.setProductionCompany(company);
            mpc.setRole(req.getRole());
            
            movieProductionCompanyRepository.save(mpc);
        }
    }

    private void validateMovieDates(MovieRequest request) {
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getReleaseDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "End date cannot be before release date", null);
        }
    }

    private void validatePublishStatus(Long movieId, MovieStatus newStatus) {
        if (newStatus == MovieStatus.UPCOMING || newStatus == MovieStatus.NOW_SHOWING) {
            List<MovieGenre> genres = movieGenreRepository.findByMovieId(movieId);
            if (genres.isEmpty()) {
                throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least 1 genre to be published", null);
            }
        }
    }

    private void validateStatusTimeConstraints(MovieStatus status, LocalDate releaseDate, LocalDate endDate) {
        if (status == null || releaseDate == null) return;
        
        LocalDate today = LocalDate.now();
        
        if (status == MovieStatus.UPCOMING) {
            if (!releaseDate.isAfter(today)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "For UPCOMING status, release date must be in the future", null);
            }
        } else if (status == MovieStatus.NOW_SHOWING) {
            if (releaseDate.isAfter(today)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "For NOW_SHOWING status, release date cannot be in the future", null);
            }
            if (endDate != null && endDate.isBefore(today)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "For NOW_SHOWING status, end date cannot be in the past", null);
            }
        } else if (status == MovieStatus.ENDED) {
            if (endDate == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "For ENDED status, end date must be provided", null);
            }
            if (!endDate.isBefore(today)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "For ENDED status, end date must be in the past", null);
            }
        }
    }
    private String generateUniqueSlug(String title) {
        if (title == null) return "";
        String normalized = Normalizer.normalize(title.trim(), Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d").replaceAll("Đ", "D")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        if (baseSlug.isEmpty()) {
            baseSlug = "movie-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        List<String> existingSlugs = movieRepository.findSlugsByPrefix(baseSlug);
        if (existingSlugs.isEmpty() || !existingSlugs.contains(baseSlug)) {
            return baseSlug;
        }

        int maxSuffix = 0;
        for (String activeSlug : existingSlugs) {
            if (activeSlug.startsWith(baseSlug + "-")) {
                try {
                    String suffixStr = activeSlug.substring(baseSlug.length() + 1);
                    int suffix = Integer.parseInt(suffixStr);
                    maxSuffix = Math.max(maxSuffix, suffix);
                } catch (NumberFormatException ignored) {}
            }
        }
        return baseSlug + "-" + (maxSuffix + 1);
    }
    
    private MovieDto mapToDto(Movie movie) {
        List<MovieGenre> movieGenres = movieGenreRepository.findByMovieId(movie.getId());
        List<String> genreNames = movieGenres.stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        return movieMapper.toDto(movie, genreNames, null);
    }

    @Transactional
    public void deleteMovie(String publicId) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        if (showtimeRepository.existsByMovieIdAndDeletedAtIsNull(movie.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot delete movie because it has active showtimes.", null);
        }
        
        Long userId = 1L;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                userId = Long.valueOf(auth.getName());
            } catch (Exception e) {}
        }

        movie.setStatus(MovieStatus.INACTIVE);
        movie.performSoftDelete(userId);
        movieRepository.save(movie);
    }
}
