package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.EmployeeDocumentResponse;
import com.project.userservice.enumtype.EmployeeDocumentType;
import com.project.userservice.service.EmployeeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/employees/{accountId}/documents")
@Tag(name = "Employee documents")
public class EmployeeDocumentController {
    private final EmployeeDocumentService service;

    public EmployeeDocumentController(EmployeeDocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Upload an employee document")
    public ResponseEntity<ApiResponse<EmployeeDocumentResponse>> upload(
            @PathVariable Long accountId,
            @RequestParam EmployeeDocumentType documentType,
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiredDate,
            @RequestParam MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.success("Employee document uploaded",
                service.upload(accountId, documentType, documentName, issuedDate, expiredDate, file)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #accountId == authentication.principal")
    @Operation(summary = "List active employee documents")
    public ResponseEntity<ApiResponse<List<EmployeeDocumentResponse>>> list(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee documents retrieved",
                service.list(accountId, false)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #accountId == authentication.principal")
    @Operation(summary = "List employee document history including deleted metadata")
    public ResponseEntity<ApiResponse<List<EmployeeDocumentResponse>>> history(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee document history retrieved",
                service.list(accountId, true)));
    }

    @GetMapping("/{documentId}/file")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #accountId == authentication.principal")
    @Operation(summary = "Download an employee document")
    public ResponseEntity<Resource> download(@PathVariable Long accountId, @PathVariable Long documentId) {
        EmployeeDocumentService.DocumentDownload download = service.download(accountId, documentId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.documentName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Delete an employee document while retaining audit metadata")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long accountId,
                                                     @PathVariable Long documentId) {
        service.delete(accountId, documentId);
        return ResponseEntity.ok(ApiResponse.success("Employee document deleted", null));
    }
}
