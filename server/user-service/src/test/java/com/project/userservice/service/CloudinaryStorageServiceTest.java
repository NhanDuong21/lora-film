package com.project.userservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.project.userservice.config.CloudinaryProperties;
import com.project.userservice.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryStorageServiceTest {
    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryStorageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey("test-key");
        properties.setApiSecret("test-secret");
        properties.setFolder("test/user-service");
        service = new CloudinaryStorageService(cloudinary, properties);
    }

    @Test
    void uploadsAvatarDirectlyToCloudinary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "public_id", "test_user-service_avatars_123",
                "secure_url", "https://res.cloudinary.com/test-cloud/image/upload/avatar.png"));

        FileStorageService.StoredFile stored = service.storeAvatar(file);

        assertThat(stored.publicId()).isEqualTo("test_user-service_avatars_123");
        assertThat(stored.fileUrl()).startsWith("https://res.cloudinary.com/");
        assertThat(stored.contentType()).isEqualTo("image/png");
        ArgumentCaptor<Map<?, ?>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), options.capture());
        assertThat(options.getValue().get("resource_type")).isEqualTo("image");
        assertThat(options.getValue().get("type")).isEqualTo("upload");
        assertThat(options.getValue().get("overwrite")).isEqualTo(false);
    }

    @Test
    void uploadsEmployeeDocumentAsAuthenticatedCloudinaryAsset() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf",
                "%PDF-1.7 test".getBytes(StandardCharsets.US_ASCII));
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "public_id", "test_user-service_employee_documents_123.pdf",
                "secure_url", "https://res.cloudinary.com/test-cloud/raw/authenticated/contract.pdf"));

        FileStorageService.StoredFile stored = service.storeEmployeeDocument(file);

        assertThat(stored.publicId()).endsWith(".pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        ArgumentCaptor<Map<?, ?>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), options.capture());
        assertThat(options.getValue().get("resource_type")).isEqualTo("raw");
        assertThat(options.getValue().get("type")).isEqualTo("authenticated");
        assertThat(options.getValue().get("public_id").toString()).endsWith(".pdf");
    }

    @Test
    void rejectsDeclaredTypeWhenMagicBytesDoNotMatch() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png",
                "not an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.storeAvatar(file))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo("USER_FILE_INVALID"))
                .hasMessageContaining("does not match");
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    void rejectsOversizedAvatarBeforeCloudinaryCall() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.storeAvatar(file))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo("USER_FILE_TOO_LARGE"));
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    void mapsCloudinaryUploadFailureToServiceUnavailable() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1});
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new IOException("Cloudinary unavailable"));

        assertThatThrownBy(() -> service.storeAvatar(file))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo("USER_STORAGE_UNAVAILABLE"));
    }

    @Test
    void deletesAvatarByCloudinaryPublicIdAndInvalidatesCdn() throws Exception {
        when(uploader.destroy(any(String.class), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        service.delete("avatars", "avatar-public-id");

        ArgumentCaptor<Map<?, ?>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(
                org.mockito.ArgumentMatchers.eq("avatar-public-id"), options.capture());
        assertThat(options.getValue().get("resource_type")).isEqualTo("image");
        assertThat(options.getValue().get("type")).isEqualTo("upload");
        assertThat(options.getValue().get("invalidate")).isEqualTo(true);
    }

    @Test
    void deletesEmployeeDocumentAsAuthenticatedRawAsset() throws Exception {
        when(uploader.destroy(any(String.class), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        service.delete("employee-documents", "document-public-id.pdf");

        ArgumentCaptor<Map<?, ?>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(
                org.mockito.ArgumentMatchers.eq("document-public-id.pdf"), options.capture());
        assertThat(options.getValue().get("resource_type")).isEqualTo("raw");
        assertThat(options.getValue().get("type")).isEqualTo("authenticated");
        assertThat(options.getValue().get("invalidate")).isEqualTo(true);
    }
}
