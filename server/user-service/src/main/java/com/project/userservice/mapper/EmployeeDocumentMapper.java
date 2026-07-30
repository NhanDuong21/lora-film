package com.project.userservice.mapper;

import com.project.userservice.dto.response.EmployeeDocumentResponse;
import com.project.userservice.entity.EmployeeDocument;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDocumentMapper {

    public EmployeeDocumentResponse toResponse(EmployeeDocument document) {
        return new EmployeeDocumentResponse(
                document.getId(),
                document.getEmployee().getAccountId(),
                document.getDocumentType(),
                document.getDocumentName(),
                document.getFileUrl(),
                document.getFileSize(),
                document.getMimeType(),
                document.getIssuedDate(),
                document.getExpiredDate(),
                document.getUploadedAt(),
                document.isDeleted(),
                document.getDeletedAt());
    }
}
