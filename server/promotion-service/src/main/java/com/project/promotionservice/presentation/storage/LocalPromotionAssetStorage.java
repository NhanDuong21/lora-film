package com.project.promotionservice.presentation.storage;

import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "promotion.assets", name = "provider",
        havingValue = "local", matchIfMissing = true)
public class LocalPromotionAssetStorage implements PromotionAssetStorage {

    private final Path uploadRoot;

    public LocalPromotionAssetStorage(
            @Value("${promotion.assets.local-directory:./uploads/promotion-campaigns}")
            String uploadDirectory) {
        uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public StoredAsset storeCover(MultipartFile file) {
        PromotionAssetFileValidator.ValidatedFile validated =
                PromotionAssetFileValidator.validate(file);
        String storageKey = UUID.randomUUID() + validated.extension();
        Path target = safePath(storageKey);
        try {
            Files.createDirectories(uploadRoot);
            Files.copy(file.getInputStream(), target);
            return new StoredAsset(
                    storageKey,
                    "/api/promotions/assets/" + storageKey,
                    validated.contentType(),
                    file.getSize());
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_STORAGE_FAILED",
                    "Unable to store campaign image");
        }
    }

    @Override
    public LoadedAsset load(String storageKey) {
        try {
            Resource resource = new UrlResource(safePath(storageKey).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(
                        "PROMOTION_ASSET_NOT_FOUND", "Campaign image was not found");
            }
            String contentType = Files.probeContentType(safePath(storageKey));
            return new LoadedAsset(resource,
                    contentType == null ? "application/octet-stream" : contentType);
        } catch (MalformedURLException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_NOT_FOUND", "Campaign image was not found");
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_STORAGE_FAILED", "Unable to read campaign image");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(safePath(storageKey));
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_STORAGE_FAILED", "Unable to delete campaign image");
        }
    }

    @Override
    public String provider() {
        return "LOCAL";
    }

    private Path safePath(String storageKey) {
        if (storageKey == null
                || !storageKey.matches("[A-Za-z0-9_-]+\\.(jpg|png|webp)")) {
            throw new BusinessException(
                    "PROMOTION_ASSET_INVALID", "Invalid campaign image identifier");
        }
        Path target = uploadRoot.resolve(storageKey).normalize();
        if (!target.startsWith(uploadRoot)
                || !target.getFileName().toString().equals(storageKey)) {
            throw new BusinessException(
                    "PROMOTION_ASSET_INVALID", "Invalid campaign image path");
        }
        return target;
    }

}
