package com.project.promotionservice.presentation.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryPromotionAssetStorageLiveTest {

    @Test
    @EnabledIfSystemProperty(
            named = "promotion.cloudinary.live-test",
            matches = "true")
    void uploadsAndCleansUpUsingConfiguredCloudinaryAccount() {
        String cloudName = requiredEnvironment("CLOUDINARY_CLOUD_NAME");
        String apiKey = requiredEnvironment("CLOUDINARY_API_KEY");
        String apiSecret = requiredEnvironment("CLOUDINARY_API_SECRET");
        CloudinaryPromotionAssetStorage storage =
                new CloudinaryPromotionAssetStorage(
                        cloudName,
                        apiKey,
                        apiSecret,
                        "lorafilm/promotions/smoke-tests");
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        MockMultipartFile file = new MockMultipartFile(
                "file", "cloudinary-smoke.png", "image/png", png);

        PromotionAssetStorage.StoredAsset uploaded = null;
        try {
            uploaded = storage.storeCover(file);
            assertThat(uploaded.storageKey())
                    .startsWith("lorafilm/promotions/smoke-tests/");
            assertThat(uploaded.url())
                    .startsWith("https://res.cloudinary.com/");
            assertThat(uploaded.contentType()).isEqualTo("image/png");
            assertThat(uploaded.bytes()).isPositive();
        } finally {
            if (uploaded != null) storage.delete(uploaded.storageKey());
        }
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for the live test");
        }
        return value;
    }
}
