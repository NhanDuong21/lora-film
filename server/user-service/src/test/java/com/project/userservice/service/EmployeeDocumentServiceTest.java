package com.project.userservice.service;

import com.project.userservice.dto.response.EmployeeDocumentResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.EmployeeDocument;
import com.project.userservice.enumtype.EmployeeDocumentType;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.EmployeeDocumentRepository;
import com.project.userservice.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDocumentServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeDocumentRepository documentRepository;
    @Mock
    private SecureFileStorageService fileStorageService;
    @Mock
    private UserAuditService auditService;
    @Mock
    private UserDomainEventService eventService;

    private EmployeeDocumentService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeDocumentService(employeeRepository, documentRepository,
                fileStorageService, auditService, eventService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(99L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadPersistsMetadataAndEmitsAuditAndOutboxEvent() {
        Employee employee = employee(42L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "C:\\fakepath\\contract.pdf", "application/pdf",
                new byte[]{'%', 'P', 'D', 'F'});
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee));
        when(fileStorageService.storeEmployeeDocument(file))
                .thenReturn(new SecureFileStorageService.StoredFile("generated.pdf", "application/pdf", 4));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(invocation -> {
            EmployeeDocument document = invocation.getArgument(0);
            ReflectionTestUtils.setField(document, "id", 7L);
            return document;
        });

        EmployeeDocumentResponse response = service.upload(
                42L, EmployeeDocumentType.LABOR_CONTRACT, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), file);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.documentName()).isEqualTo("contract.pdf");
        assertThat(response.fileUrl()).endsWith("/employees/42/documents/7/file");
        verify(auditService).log("EMPLOYEE_DOCUMENT_UPLOADED", "EMPLOYEE_DOCUMENT", 7L,
                "employeeId=42, type=LABOR_CONTRACT");
        verify(eventService).record(
                org.mockito.ArgumentMatchers.eq("EMPLOYEE_DOCUMENT_UPLOADED"),
                org.mockito.ArgumentMatchers.eq("EMPLOYEE"),
                org.mockito.ArgumentMatchers.eq(42L),
                any());
    }

    @Test
    void uploadRejectsExpiryBeforeIssueWithoutWritingFile() {
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee(42L)));
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        assertThatThrownBy(() -> service.upload(
                42L, EmployeeDocumentType.LABOR_CONTRACT, "Contract",
                LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1), file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be before");

        verify(fileStorageService, never()).storeEmployeeDocument(any());
    }

    @Test
    void deleteSoftDeletesMetadataAndRemovesStoredFile() {
        EmployeeDocument document = new EmployeeDocument();
        ReflectionTestUtils.setField(document, "id", 7L);
        document.setEmployee(employee(42L));
        document.setDocumentType(EmployeeDocumentType.CERTIFICATE);
        document.setDocumentName("Certificate");
        document.setFileName("generated.pdf");
        document.setFileUrl("/api/users/employees/42/documents/7/file");
        document.setFileSize(4L);
        document.setMimeType("application/pdf");
        when(documentRepository.findByIdAndEmployeeAccountIdAndDeletedAtIsNull(7L, 42L))
                .thenReturn(Optional.of(document));

        service.delete(42L, 7L);

        assertThat(document.isDeleted()).isTrue();
        assertThat(document.getDeletedBy()).isEqualTo(99L);
        verify(fileStorageService).deleteAfterCommit("employee-documents", "generated.pdf");
        verify(documentRepository).save(document);
        verify(eventService).record(
                org.mockito.ArgumentMatchers.eq("EMPLOYEE_DOCUMENT_DELETED"),
                org.mockito.ArgumentMatchers.eq("EMPLOYEE"),
                org.mockito.ArgumentMatchers.eq(42L),
                any());
    }

    private Employee employee(Long accountId) {
        Employee employee = new Employee();
        employee.setAccountId(accountId);
        return employee;
    }
}
