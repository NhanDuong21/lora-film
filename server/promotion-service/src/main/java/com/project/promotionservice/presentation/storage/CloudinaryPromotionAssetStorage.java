package com.project.promotionservice.presentation.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "promotion.assets", name = "provider",
        havingValue = "cloudinary")
public class CloudinaryPromotionAssetStorage implements PromotionAssetStorage {

    private final Uploader uploader;
    private final String folder;

    @Autowired
    public CloudinaryPromotionAssetStorage(
            @Value("${promotion.assets.cloudinary.cloud-name:}") String cloudName,
            @Value("${promotion.assets.cloudinary.api-key:}") String apiKey,
            @Value("${promotion.assets.cloudinary.api-secret:}") String apiSecret,
            @Value("${promotion.assets.cloudinary.folder:lorafilm/promotions/campaigns}")
            String folder) {
        this(createCloudinary(cloudName, apiKey, apiSecret).uploader(), folder);
    }

    CloudinaryPromotionAssetStorage(Uploader uploader, String folder) {
        this.uploader = uploader;
        this.folder = normalizeFolder(folder);
    }

    @Override
    public StoredAsset storeCover(MultipartFile file) {
        PromotionAssetFileValidator.ValidatedFile validated =
                PromotionAssetFileValidator.validate(file);
        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "type", "upload",
                "public_id", UUID.randomUUID().toString(),
                "overwrite", false);
        try {
            Map<?, ?> result = uploader.upload(file.getBytes(), uploadParams);
            String publicId = requiredString(result, "public_id");
            String secureUrl = requiredString(result, "secure_url");
            Number bytes = result.get("bytes") instanceof Number number
                    ? number
                    : file.getSize();
            return new StoredAsset(
                    publicId,
                    secureUrl,
                    validated.contentType(),
                    bytes.longValue());
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_STORAGE_FAILED",
                    "Unable to upload campaign image to Cloudinary");
        }
    }

    @Override
    public LoadedAsset load(String storageKey) {
        throw new BusinessException(
                "PROMOTION_ASSET_DIRECT_URL_REQUIRED",
                "Cloudinary campaign images must be loaded from their secure URL");
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        try {
            Map<?, ?> result = uploader.destroy(storageKey, ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "upload",
                    "invalidate", true));
            Object outcome = result.get("result");
            if (outcome != null
                    && !"ok".equalsIgnoreCase(outcome.toString())
                    && !"not found".equalsIgnoreCase(outcome.toString())) {
                throw new BusinessException(
                        "PROMOTION_ASSET_STORAGE_FAILED",
                        "Cloudinary did not delete the campaign image");
            }
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_STORAGE_FAILED",
                    "Unable to delete campaign image from Cloudinary");
        }
    }

    @Override
    public String provider() {
        return "CLOUDINARY";
    }

    private static Cloudinary createCloudinary(
            String cloudName,
            String apiKey,
            String apiSecret) {
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException(
                    "Cloudinary credentials are required when promotion.assets.provider=cloudinary");
        }
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName.trim(),
                "api_key", apiKey.trim(),
                "api_secret", apiSecret.trim(),
                "secure", true));
    }

    private static String normalizeFolder(String value) {
        if (isBlank(value)) return "lorafilm/promotions/campaigns";
        String normalized = value.trim().replace('\\', '/');
        normalized = normalized.replaceAll("^/+|/+$", "");
        if (!normalized.matches("[A-Za-z0-9/_-]+") || normalized.contains("//")) {
            throw new IllegalArgumentException("Invalid Cloudinary promotion folder");
        }
        return normalized;
    }

    private static String requiredString(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new BusinessException(
                    "PROMOTION_ASSET_STORAGE_FAILED",
                    "Cloudinary returned an incomplete upload response");
        }
        return value.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
