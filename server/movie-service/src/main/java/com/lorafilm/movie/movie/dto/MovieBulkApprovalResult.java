package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public record MovieBulkApprovalResult(
        String moviePublicId,
        String title,
        String outcome,
        MovieStatus newStatus,
        String reasonCode,
        String reason) {

    public static MovieBulkApprovalResult approved(String publicId, String title, MovieStatus newStatus) {
        return new MovieBulkApprovalResult(publicId, title, "APPROVED", newStatus, null, null);
    }

    public static MovieBulkApprovalResult skipped(
            String publicId,
            String title,
            String reasonCode,
            String reason) {
        return new MovieBulkApprovalResult(publicId, title, "SKIPPED", null, reasonCode, reason);
    }

    public static MovieBulkApprovalResult error(
            String publicId,
            String title,
            String reasonCode,
            String reason) {
        return new MovieBulkApprovalResult(publicId, title, "ERROR", null, reasonCode, reason);
    }
}
