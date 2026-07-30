package com.project.userservice.service;

import com.project.userservice.dto.response.EmployeeDocumentResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.EmployeeDocument;
import com.project.userservice.enumtype.EmployeeDocumentType;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.mapper.EmployeeDocumentMapper;
import com.project.userservice.repository.EmployeeDocumentRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.security.CurrentActor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeDocumentService {
    private static final String STORAGE_DIRECTORY = "employee-documents";

    private final EmployeeRepository employeeRepository;
    private final EmployeeDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;
    private final EmployeeDocumentMapper documentMapper;

    public EmployeeDocumentService(EmployeeRepository employeeRepository,
                                   EmployeeDocumentRepository documentRepository,
                                   FileStorageService fileStorageService,
                                   UserAuditService auditService,
                                   UserDomainEventService eventService,
                                   EmployeeDocumentMapper documentMapper) {
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.eventService = eventService;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public EmployeeDocumentResponse upload(Long accountId, EmployeeDocumentType documentType,
                                           String documentName, LocalDate issuedDate,
                                           LocalDate expiredDate, MultipartFile file) {
        Employee employee = findEmployee(accountId);
        validateDates(issuedDate, expiredDate);
        String normalizedName = normalizeDocumentName(documentName, file);
        FileStorageService.StoredFile storedFile = fileStorageService.storeEmployeeDocument(file);
        fileStorageService.deleteOnRollback(STORAGE_DIRECTORY, storedFile.publicId());

        try {
            EmployeeDocument document = new EmployeeDocument();
            document.setEmployee(employee);
            document.setDocumentType(documentType);
            document.setDocumentName(normalizedName);
            document.setFileName(storedFile.publicId());
            document.setFileUrl("/api/users/employees/" + accountId + "/documents");
            document.setFileSize(storedFile.fileSize());
            document.setMimeType(storedFile.contentType());
            document.setIssuedDate(issuedDate);
            document.setExpiredDate(expiredDate);
            document.setUploadedBy(CurrentActor.accountId());
            document = documentRepository.save(document);
            document.setFileUrl(document.getFileUrl() + "/" + document.getId() + "/file");

            auditService.log("EMPLOYEE_DOCUMENT_UPLOADED", "EMPLOYEE_DOCUMENT", document.getId(),
                    "employeeId=" + accountId + ", type=" + documentType);
            eventService.record("EMPLOYEE_DOCUMENT_UPLOADED", "EMPLOYEE", accountId,
                    eventData(document));
            return documentMapper.toResponse(document);
        } catch (RuntimeException exception) {
            try {
                fileStorageService.delete(STORAGE_DIRECTORY, storedFile.publicId());
            } catch (RuntimeException ignored) {
                // Preserve the database or business failure that prevented metadata persistence.
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponse> list(Long accountId, boolean includeDeleted) {
        findEmployee(accountId);
        List<EmployeeDocument> documents = includeDeleted
                ? documentRepository.findByEmployeeAccountIdOrderByUploadedAtDesc(accountId)
                : documentRepository.findByEmployeeAccountIdAndDeletedAtIsNullOrderByUploadedAtDesc(accountId);
        return documents.stream().map(documentMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(Long accountId, Long documentId) {
        EmployeeDocument document = findActiveDocument(accountId, documentId);
        return new DocumentDownload(
                fileStorageService.load(STORAGE_DIRECTORY, document.getFileName()),
                document.getMimeType(),
                document.getDocumentName(),
                document.getFileSize());
    }

    @Transactional
    public void delete(Long accountId, Long documentId) {
        EmployeeDocument document = findActiveDocument(accountId, documentId);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(CurrentActor.accountId());
        documentRepository.save(document);
        auditService.log("EMPLOYEE_DOCUMENT_DELETED", "EMPLOYEE_DOCUMENT", documentId,
                "employeeId=" + accountId);
        eventService.record("EMPLOYEE_DOCUMENT_DELETED", "EMPLOYEE", accountId,
                eventData(document));
        fileStorageService.deleteAfterCommit(STORAGE_DIRECTORY, document.getFileName());
    }

    private Employee findEmployee(Long accountId) {
        return employeeRepository.findById(accountId)
                .filter(employee -> !employee.isDeleted())
                .orElseThrow(() -> new BusinessException("Employee not found", "USER_EMPLOYEE_NOT_FOUND"));
    }

    private EmployeeDocument findActiveDocument(Long accountId, Long documentId) {
        return documentRepository.findByIdAndEmployeeAccountIdAndDeletedAtIsNull(documentId, accountId)
                .orElseThrow(() -> new BusinessException("Employee document not found",
                        "USER_EMPLOYEE_DOCUMENT_NOT_FOUND"));
    }

    private void validateDates(LocalDate issuedDate, LocalDate expiredDate) {
        if (issuedDate != null && expiredDate != null && expiredDate.isBefore(issuedDate)) {
            throw new BusinessException("Document expiry date cannot be before its issue date",
                    "USER_EMPLOYEE_DOCUMENT_DATES");
        }
    }

    private String normalizeDocumentName(String documentName, MultipartFile file) {
        String candidate = documentName == null || documentName.isBlank()
                ? file == null ? null : file.getOriginalFilename()
                : documentName;
        if (candidate == null || candidate.isBlank()) {
            throw new BusinessException("Document name is required", "USER_EMPLOYEE_DOCUMENT_NAME");
        }
        String pathFreeName = candidate.replace('\\', '/');
        pathFreeName = pathFreeName.substring(pathFreeName.lastIndexOf('/') + 1);
        String normalized = pathFreeName.replaceAll("[\\p{Cntrl}]", "").trim();
        if (normalized.isEmpty() || normalized.length() > 255) {
            throw new BusinessException("Document name must contain 1 to 255 characters",
                    "USER_EMPLOYEE_DOCUMENT_NAME");
        }
        return normalized;
    }

    private Map<String, Object> eventData(EmployeeDocument document) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("employeeId", document.getEmployee().getAccountId());
        data.put("documentId", document.getId());
        data.put("documentType", document.getDocumentType().name());
        data.put("documentName", document.getDocumentName());
        data.put("deleted", document.isDeleted());
        return data;
    }

    public record DocumentDownload(Resource resource, String contentType, String documentName, long fileSize) {
    }
}
