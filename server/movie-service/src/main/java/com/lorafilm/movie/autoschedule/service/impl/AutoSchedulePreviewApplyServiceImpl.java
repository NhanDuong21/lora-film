package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.mapper.AutoScheduleApplyResponseMapper;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleApplyRevalidationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleAuditoriumLockService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewApplyService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AutoSchedulePreviewApplyServiceImpl implements AutoSchedulePreviewApplyService {

    private static final Logger log = LoggerFactory.getLogger(AutoSchedulePreviewApplyServiceImpl.class);

    private final CurrentUserProvider currentUserProvider;
    private final ShowtimeSchedulePreviewExpiryService expiryService;
    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository itemRepository;
    private final AutoScheduleAuditoriumLockService auditoriumLockService;
    private final AutoScheduleApplyRevalidationService revalidationService;
    private final AutoScheduleShowtimeCreationService showtimeCreationService;
    private final AutoScheduleApplyResponseMapper responseMapper;
    private final Clock clock;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final MovieRepository movieRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final EntityManager entityManager;

    public AutoSchedulePreviewApplyServiceImpl(CurrentUserProvider currentUserProvider,
                                               ShowtimeSchedulePreviewExpiryService expiryService,
                                               ShowtimeSchedulePreviewRepository previewRepository,
                                               ShowtimeSchedulePreviewItemRepository itemRepository,
                                               AutoScheduleAuditoriumLockService auditoriumLockService,
                                               AutoScheduleApplyRevalidationService revalidationService,
                                               AutoScheduleShowtimeCreationService showtimeCreationService,
                                               AutoScheduleApplyResponseMapper responseMapper,
                                               Clock clock,
                                               org.springframework.transaction.support.TransactionTemplate transactionTemplate,
                                               MovieRepository movieRepository,
                                               MovieVersionRepository movieVersionRepository,
                                               CinemaRepository cinemaRepository,
                                               AuditoriumRepository auditoriumRepository,
                                               EntityManager entityManager) {
        this.currentUserProvider = currentUserProvider;
        this.expiryService = expiryService;
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.auditoriumLockService = auditoriumLockService;
        this.revalidationService = revalidationService;
        this.showtimeCreationService = showtimeCreationService;
        this.responseMapper = responseMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.movieRepository = movieRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.entityManager = entityManager;
    }

    @Override
    public ApplyShowtimeSchedulePreviewResponse applyPreview(String previewPublicId, ApplyShowtimeSchedulePreviewRequest request) {
        Long actorId = currentUserProvider.getCurrentUserId();
        if (actorId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE, "Current user not available");
        }

        String applyKey = request.getIdempotencyKey().trim();
        Instant now = Instant.now(clock);

        // Normalize expiry in a separate transaction
        if (expiryService.expireIfNecessary(previewPublicId, now)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
        }

        try {
            return transactionTemplate.execute(status -> doApply(previewPublicId, applyKey, request.getExpectedVersion(), actorId));
        } catch (DataIntegrityViolationException e) {
            return handleIdempotencyConflict(applyKey, previewPublicId);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED) {
                normalizeExpiryAfterRollback(previewPublicId);
            }
            throw e;
        }
    }

    ApplyShowtimeSchedulePreviewResponse doApply(String previewPublicId, String applyKey, Long expectedVersion, Long actorId) {
        // Check if key already exists in the same transaction context (idempotency check before doing work)
        Optional<ShowtimeSchedulePreview> existingByKey = previewRepository.findByApplyIdempotencyKeyDetailed(applyKey);
        if (existingByKey.isPresent()) {
            return handleIdempotencyConflict(applyKey, previewPublicId, existingByKey.get());
        }

        // Lock the preview using pessimistic lock to prevent concurrent modifications
        ShowtimeSchedulePreview preview = previewRepository.findByPublicIdForApply(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        // Re-check idempotency key and status AFTER acquiring lock (to handle same-key concurrency)
        if (applyKey.equals(preview.getApplyIdempotencyKey())) {
            if (preview.getStatus() == SchedulePreviewStatus.APPLIED) {
                return responseMapper.toResponse(preview);
            }
            if (preview.getStatus() == SchedulePreviewStatus.APPLYING) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
            }
        }
        
        if (preview.getApplyIdempotencyKey() != null && !applyKey.equals(preview.getApplyIdempotencyKey())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_ALREADY_APPLIED);
        }

        validateStateForApply(preview, expectedVersion);

        List<ShowtimeSchedulePreviewItemRepository.ApplyItemReference> selectedReferences =
                loadSelectedReferences(preview);

        List<Long> auditoriumIds = selectedReferences.stream()
                .map(ShowtimeSchedulePreviewItemRepository.ApplyItemReference::getAuditoriumId)
                .collect(Collectors.toList());

        auditoriumLockService.lockAll(auditoriumIds);

        Instant freshNow = Instant.now(clock);
        validateFreshExpiry(preview, freshNow);

        ReloadedApplyState currentState = reloadAuthoritativeState(previewPublicId, expectedVersion);
        preview = currentState.preview();
        List<ShowtimeSchedulePreviewItem> selectedItems = currentState.selectedItems();

        revalidationService.validateAll(preview, selectedItems, freshNow);

        // State changes
        preview.markApplying(applyKey);

        // Create showtimes
        List<Showtime> createdShowtimes = showtimeCreationService.createAll(selectedItems, actorId, previewPublicId);

        // Update items
        for (int i = 0; i < selectedItems.size(); i++) {
            ShowtimeSchedulePreviewItem item = selectedItems.get(i);
            Showtime showtime = createdShowtimes.get(i);
            item.setApplyStatus(PreviewItemApplyStatus.CREATED);
            item.setCreatedShowtime(showtime);
            item.setApplyErrorCode(null);
            item.setApplyErrorMessage(null);
        }

        // Unselected / rejected items -> SKIPPED
        List<ShowtimeSchedulePreviewItem> allItems = itemRepository.findDetailedItemsByPreviewId(preview.getId());
        for (ShowtimeSchedulePreviewItem item : allItems) {
            if (item.getApplyStatus() == PreviewItemApplyStatus.PENDING) {
                item.setApplyStatus(PreviewItemApplyStatus.SKIPPED);
            }
        }

        preview.markApplied(actorId, Instant.now(clock));
        
        return responseMapper.toResponse(preview);
    }

    private ApplyShowtimeSchedulePreviewResponse handleIdempotencyConflict(String applyKey, String previewPublicId) {
        ShowtimeSchedulePreview preview = previewRepository.findByApplyIdempotencyKeyDetailed(applyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency key reused but not found?"));
        return handleIdempotencyConflict(applyKey, previewPublicId, preview);
    }

    private ApplyShowtimeSchedulePreviewResponse handleIdempotencyConflict(String applyKey, String previewPublicId, ShowtimeSchedulePreview preview) {
        if (!preview.getPublicId().equals(previewPublicId)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency key was reused with a different preview");
        }

        if (preview.getStatus() == SchedulePreviewStatus.APPLIED) {
            return responseMapper.toResponse(preview);
        } else if (preview.getStatus() == SchedulePreviewStatus.APPLYING) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
        } else {
            // It could be FAILED or CANCELLED with that key. If it's failed, maybe the key is stuck.
            // But requirement says: FAILED/CANCELLED/EXPIRED -> không tự replay thành success.
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }
    }

    private List<ShowtimeSchedulePreviewItemRepository.ApplyItemReference> loadSelectedReferences(
            ShowtimeSchedulePreview preview) {
        List<ShowtimeSchedulePreviewItemRepository.ApplyItemReference> selectedReferences =
                itemRepository.findSelectedItemReferencesForApply(preview.getId(), PreviewItemValidationStatus.VALID);
        if (selectedReferences.isEmpty() || preview.getSelectedCandidateCount() == 0) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_NO_SELECTED_ITEMS, "No selected items to apply");
        }
        if (selectedReferences.size() != preview.getSelectedCandidateCount()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT, "Selected item count mismatch");
        }
        return selectedReferences;
    }

    private ReloadedApplyState reloadAuthoritativeState(String previewPublicId, Long expectedVersion) {
        entityManager.clear();

        ShowtimeSchedulePreview currentPreview = previewRepository.findByPublicIdForApply(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));
        validateStateForApply(currentPreview, expectedVersion);

        List<ShowtimeSchedulePreviewItemRepository.ApplyItemReference> references = loadSelectedReferences(currentPreview);
        List<Long> itemIds = references.stream()
                .map(ShowtimeSchedulePreviewItemRepository.ApplyItemReference::getItemId)
                .toList();

        Map<Long, ShowtimeSchedulePreviewItem> itemsById = mapById(
                itemRepository.findAllById(itemIds), ShowtimeSchedulePreviewItem::getId);
        Map<Long, Movie> moviesById = mapById(
                movieRepository.findAllById(distinctIds(references, ShowtimeSchedulePreviewItemRepository.ApplyItemReference::getMovieId)),
                Movie::getId);
        Map<Long, MovieVersion> versionsById = mapById(
                movieVersionRepository.findAllById(distinctIds(references, ShowtimeSchedulePreviewItemRepository.ApplyItemReference::getMovieVersionId)),
                MovieVersion::getId);
        Map<Long, Cinema> cinemasById = mapById(
                cinemaRepository.findAllById(distinctIds(references, ShowtimeSchedulePreviewItemRepository.ApplyItemReference::getCinemaId)),
                Cinema::getId);
        Map<Long, Auditorium> auditoriumsById = mapById(
                auditoriumRepository.findAllById(distinctIds(references, ShowtimeSchedulePreviewItemRepository.ApplyItemReference::getAuditoriumId)),
                Auditorium::getId);

        List<ShowtimeSchedulePreviewItem> currentItems = new ArrayList<>(references.size());
        for (ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference : references) {
            ShowtimeSchedulePreviewItem item = itemsById.get(reference.getItemId());
            if (item == null) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT);
            }

            Movie movie = requireCurrent(moviesById, reference.getMovieId(), ErrorCode.MOVIE_NOT_FOUND);
            MovieVersion version = requireCurrent(
                    versionsById, reference.getMovieVersionId(), ErrorCode.MOVIE_VERSION_NOT_FOUND);
            Cinema cinema = requireCurrent(cinemasById, reference.getCinemaId(), ErrorCode.CINEMA_NOT_FOUND);
            Auditorium auditorium = requireCurrent(
                    auditoriumsById, reference.getAuditoriumId(), ErrorCode.AUDITORIUM_NOT_FOUND);

            validateNotDeleted(movie, version, cinema, auditorium);
            validateOwnership(currentPreview, movie, version, cinema, auditorium);

            item.setMovie(movie);
            item.setMovieVersion(version);
            item.setCinema(cinema);
            item.setAuditorium(auditorium);
            currentItems.add(item);
        }

        return new ReloadedApplyState(currentPreview, currentItems);
    }

    private void validateNotDeleted(Movie movie,
                                    MovieVersion version,
                                    Cinema cinema,
                                    Auditorium auditorium) {
        if (movie.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }
        if (version.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND);
        }
        if (cinema.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        if (auditorium.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND);
        }
    }

    private void validateOwnership(ShowtimeSchedulePreview preview,
                                   Movie movie,
                                   MovieVersion version,
                                   Cinema cinema,
                                   Auditorium auditorium) {
        if (version.getMovie() == null || !Objects.equals(version.getMovie().getId(), movie.getId())) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_BELONG_TO_MOVIE);
        }
        if (auditorium.getCinema() == null || !Objects.equals(auditorium.getCinema().getId(), cinema.getId())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA);
        }
        if (preview.getCinema() == null || !Objects.equals(preview.getCinema().getId(), cinema.getId())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT);
        }
    }

    private <T> T requireCurrent(Map<Long, T> entities, Long id, ErrorCode errorCode) {
        T entity = id == null ? null : entities.get(id);
        if (entity == null) {
            throw new BusinessException(errorCode);
        }
        return entity;
    }

    private <T> Map<Long, T> mapById(Iterable<T> entities, Function<T, Long> idExtractor) {
        Map<Long, T> result = new java.util.LinkedHashMap<>();
        for (T entity : entities) {
            result.put(idExtractor.apply(entity), entity);
        }
        return result;
    }

    private List<Long> distinctIds(
            List<ShowtimeSchedulePreviewItemRepository.ApplyItemReference> references,
            Function<ShowtimeSchedulePreviewItemRepository.ApplyItemReference, Long> extractor) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference : references) {
            Long id = extractor.apply(reference);
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private void validateFreshExpiry(ShowtimeSchedulePreview preview, Instant freshNow) {
        if (!freshNow.isBefore(preview.getExpiresAt())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
        }
    }

    private void normalizeExpiryAfterRollback(String previewPublicId) {
        try {
            expiryService.expireIfNecessary(previewPublicId, Instant.now(clock));
        } catch (Exception normalizationFailure) {
            log.error("Could not normalize expired auto schedule preview {} after apply rollback",
                    previewPublicId, normalizationFailure);
        }
    }

    private void validateStateForApply(ShowtimeSchedulePreview preview, Long expectedVersion) {
        if (!preview.getVersion().equals(expectedVersion)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
        }

        if (preview.getApplyMode() != SchedulePreviewApplyMode.ALL_OR_NOTHING) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }

        switch (preview.getStatus()) {
            case PREVIEWED:
                break;
            case GENERATING:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
            case APPLYING:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
            case APPLIED:
                // If it was applied, and we reached here (didn't match idempotency key earlier)
                // it means it was applied with a different key.
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_ALREADY_APPLIED);
            case EXPIRED:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
            case FAILED:
            case CANCELLED:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }
    }

    private record ReloadedApplyState(ShowtimeSchedulePreview preview,
                                      List<ShowtimeSchedulePreviewItem> selectedItems) {
    }
}
