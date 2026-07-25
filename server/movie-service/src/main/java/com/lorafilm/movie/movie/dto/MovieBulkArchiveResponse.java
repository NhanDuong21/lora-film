package com.lorafilm.movie.movie.dto;

import java.util.List;

public record MovieBulkArchiveResponse(
        int requested,
        int archived,
        int skipped,
        int errors,
        int limit,
        List<MovieBulkApprovalResult> results) {
}
