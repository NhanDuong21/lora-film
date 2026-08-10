package com.lorafilm.movie.integration.tmdb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbBulkSyncRequest;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImportResult;
import com.lorafilm.movie.integration.tmdb.dto.TmdbSyncStateDto;
import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;
import com.lorafilm.movie.integration.tmdb.service.TmdbSyncJobLauncher;
import com.lorafilm.movie.integration.tmdb.service.TmdbSyncStateQueryService;

@RestController
@RequestMapping("/api/admin/tmdb")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class TmdbAdminController {

    private static final Logger log = LoggerFactory.getLogger(TmdbAdminController.class);

    private final TmdbImportService tmdbImportService;
    private final TmdbSyncStateQueryService syncStateQueryService;
    private final TmdbSyncJobLauncher syncJobLauncher;

    public TmdbAdminController(TmdbImportService tmdbImportService,
                               TmdbSyncStateQueryService syncStateQueryService,
                               TmdbSyncJobLauncher syncJobLauncher) {
        this.tmdbImportService = tmdbImportService;
        this.syncStateQueryService = syncStateQueryService;
        this.syncJobLauncher = syncJobLauncher;
    }

    @GetMapping("/sync/state")
    public ResponseEntity<ApiResponse<TmdbSyncStateDto>> getSyncState() {
        log.info("[TmdbAdminController] Request to get sync state");
        TmdbSyncStateDto dto = syncStateQueryService.getSyncState("TMDB_BULK_EXPORT");
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/sync/{tmdbId}")
    public ResponseEntity<ApiResponse<String>> syncMovieById(@PathVariable Long tmdbId) {
        log.info("[TmdbAdminController] Request to sync movie by ID: {}", tmdbId);
        TmdbImportResult result = tmdbImportService.importMovieById(tmdbId);
        log.info("[TmdbAdminController] TMDB sync outcome for {}: {}", tmdbId, result.outcome());
        String message = switch (result.outcome()) {
            case CREATED -> "Đã nhập phim vào danh sách Chờ hoàn thiện.";
            case ALREADY_IMPORTED -> "Phim đã tồn tại trong hệ thống nên không tạo bản trùng lặp.";
            case DELETED_TOMBSTONE -> "Phim này từng bị loại khỏi hệ thống nên chưa được nhập lại.";
            case REJECTED_BY_PROVIDER -> "Nguồn TMDB từ chối cung cấp phim này. Vui lòng kiểm tra lại mã phim.";
        };
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @PostMapping("/sync/bulk/start")
    public ResponseEntity<ApiResponse<String>> startBulkSync(
            @Valid @RequestBody(required = false) TmdbBulkSyncRequest request) {
        log.info("[TmdbAdminController] Request to start bulk export sync");
        TmdbBulkSyncRequest resolved = request == null ? TmdbBulkSyncRequest.futureDefault() : request;
        tmdbImportService.validateBulkSyncRequest(resolved);
        syncJobLauncher.start(resolved, false);
        return ResponseEntity.ok(ApiResponse.ok("Đã bắt đầu nhập phim trong nền."));
    }

    @PostMapping("/sync/bulk/stop")
    public ResponseEntity<ApiResponse<String>> stopBulkSync() {
        log.info("[TmdbAdminController] Request to stop bulk export sync");
        tmdbImportService.stopBulkSync();
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi yêu cầu dừng tiến trình nhập phim."));
    }

    @GetMapping("/sync/bulk/status")
    public ResponseEntity<ApiResponse<TmdbSyncStateDto>> getBulkSyncStatus() {
        return ResponseEntity.ok(ApiResponse.ok(syncStateQueryService.getSyncState("TMDB_BULK_EXPORT")));
    }

    @PostMapping("/sync/bulk/reset")
    public ResponseEntity<ApiResponse<String>> resetBulkSync(
            @Valid @RequestBody(required = false) TmdbBulkSyncRequest request) {
        log.info("[TmdbAdminController] Request to reset bulk export sync cursor and start background thread");
        TmdbBulkSyncRequest resolved = request == null ? TmdbBulkSyncRequest.futureDefault() : request;
        tmdbImportService.validateBulkSyncRequest(resolved);
        syncJobLauncher.start(resolved, true);
        return ResponseEntity.ok(ApiResponse.ok("Đã chạy lại tiến trình từ đầu với phạm vi đã chọn."));
    }
}
