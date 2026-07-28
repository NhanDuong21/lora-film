package com.project.userservice.service;

import com.project.userservice.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureFileStorageServiceTest {
    @TempDir
    Path uploadDirectory;

    @Test
    void storesLoadsAndDeletesPdfByGeneratedName() throws Exception {
        SecureFileStorageService service = new SecureFileStorageService(uploadDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf",
                "%PDF-1.7 test document".getBytes(StandardCharsets.US_ASCII));

        SecureFileStorageService.StoredFile stored = service.storeEmployeeDocument(file);

        assertThat(stored.fileName()).endsWith(".pdf").doesNotContain("contract");
        assertThat(service.load("employee-documents", stored.fileName()).contentLength())
                .isEqualTo(file.getSize());
        service.delete("employee-documents", stored.fileName());
        assertThat(Files.exists(uploadDirectory.resolve("employee-documents").resolve(stored.fileName())))
                .isFalse();
    }

    @Test
    void rejectsDeclaredTypeWhenMagicBytesDoNotMatch() {
        SecureFileStorageService service = new SecureFileStorageService(uploadDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.storeEmployeeDocument(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsTraversalWhenLoadingStoredFiles() {
        SecureFileStorageService service = new SecureFileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() -> service.load("employee-documents", "../secret.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid file path");
    }

    @Test
    void acceptsDocxPackageAndRejectsGenericZipWithSpoofedMimeType() throws Exception {
        SecureFileStorageService service = new SecureFileStorageService(uploadDirectory.toString());
        MockMultipartFile docx = new MockMultipartFile(
                "file", "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                zip("[Content_Types].xml", "word/document.xml"));
        MockMultipartFile genericZip = new MockMultipartFile(
                "file", "fake.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                zip("payload.txt"));

        SecureFileStorageService.StoredFile stored = service.storeEmployeeDocument(docx);

        assertThat(stored.fileName()).endsWith(".docx");
        assertThatThrownBy(() -> service.storeEmployeeDocument(genericZip))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DOCX package");
    }

    private byte[] zip(String... entryNames) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String entryName : entryNames) {
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write("test".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
