package com.project.userservice.service;

import com.project.userservice.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {
    @TempDir
    Path uploadDirectory;

    @Test
    void storesLoadsAndDeletesPdfByGeneratedName() throws Exception {
        LocalFileStorageService service =
                new LocalFileStorageService(uploadDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf",
                "%PDF-1.7 test document".getBytes(StandardCharsets.US_ASCII));

        FileStorageService.StoredFile stored = service.storeEmployeeDocument(file);

        assertThat(stored.publicId()).endsWith(".pdf").doesNotContain("contract");
        assertThat(service.load("employee-documents", stored.publicId()).contentLength())
                .isEqualTo(file.getSize());
        service.delete("employee-documents", stored.publicId());
        assertThat(Files.exists(uploadDirectory.resolve("employee-documents")
                .resolve(stored.publicId()))).isFalse();
    }

    @Test
    void createsGatewayRelativeAvatarUrl() {
        LocalFileStorageService service =
                new LocalFileStorageService(uploadDirectory.toString());
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        };
        MockMultipartFile file =
                new MockMultipartFile("file", "avatar.png", "image/png", png);

        FileStorageService.StoredFile stored = service.storeAvatar(file);

        assertThat(stored.fileUrl())
                .isEqualTo("/api/users/profile/avatar/files/" + stored.publicId());
    }

    @Test
    void rejectsDeclaredTypeWhenMagicBytesDoNotMatch() {
        LocalFileStorageService service =
                new LocalFileStorageService(uploadDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.storeEmployeeDocument(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsTraversalWhenLoadingStoredFiles() {
        LocalFileStorageService service =
                new LocalFileStorageService(uploadDirectory.toString());

        assertThatThrownBy(() ->
                service.load("employee-documents", "../secret.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid storage identifier");
    }
}
