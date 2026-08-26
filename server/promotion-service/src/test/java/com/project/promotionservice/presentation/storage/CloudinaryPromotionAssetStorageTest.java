package com.project.promotionservice.presentation.storage;

import com.cloudinary.Uploader;
import com.project.promotionservice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryPromotionAssetStorageTest {

    @Mock
    private Uploader uploader;

    @Test
    void uploadsValidatedImageAndReturnsCloudinaryReference() throws Exception {
        CloudinaryPromotionAssetStorage storage =
                new CloudinaryPromotionAssetStorage(
                        uploader, "lorafilm/promotions/campaigns");
        MockMultipartFile file = pngFile();
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "public_id", "lorafilm/promotions/campaigns/cover-1",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/cover-1.png",
                "bytes", 512L));

        PromotionAssetStorage.StoredAsset stored = storage.storeCover(file);

        assertThat(stored.storageKey())
                .isEqualTo("lorafilm/promotions/campaigns/cover-1");
        assertThat(stored.url()).startsWith("https://res.cloudinary.com/");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.bytes()).isEqualTo(512L);
        assertThat(storage.provider()).isEqualTo("CLOUDINARY");
    }

    @Test
    void rejectsSpoofedImageBeforeCallingCloudinary() {
        CloudinaryPromotionAssetStorage storage =
                new CloudinaryPromotionAssetStorage(
                        uploader, "lorafilm/promotions/campaigns");
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> storage.storeCover(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void destroysCloudinaryAssetByPublicId() throws Exception {
        CloudinaryPromotionAssetStorage storage =
                new CloudinaryPromotionAssetStorage(
                        uploader, "lorafilm/promotions/campaigns");
        String publicId = "lorafilm/promotions/campaigns/cover-1";
        when(uploader.destroy(eq(publicId), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        storage.delete(publicId);

        verify(uploader).destroy(eq(publicId), anyMap());
    }

    private MockMultipartFile pngFile() {
        byte[] bytes = new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47,
                0x0d, 0x0a, 0x1a, 0x0a,
                0x00, 0x00, 0x00, 0x00};
        return new MockMultipartFile(
                "file", "cover.png", "image/png", bytes);
    }
}
