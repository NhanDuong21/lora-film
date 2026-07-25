package com.lorafilm.movie.movie.dto;

import java.util.List;

public record MovieBulkApprovalResponse(
        int requested,
        int approved,
        int skipped,
        int errors,
        int limit,
        List<MovieBulkApprovalResult> results) {
}
