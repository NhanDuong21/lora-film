package com.project.userservice.dto.response;

import com.project.userservice.enumtype.EmployeeDocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeDocumentResponse(
        Long id,
        Long employeeId,
        EmployeeDocumentType documentType,
        String documentName,
        String fileUrl,
        Long fileSize,
        String mimeType,
        LocalDate issuedDate,
        LocalDate expiredDate,
        LocalDateTime uploadedAt,
        boolean deleted,
        LocalDateTime deletedAt
) {
}
