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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@ConditionalOnProperty(prefix = "promotion.assets", name = "provider",
        havingValue = "local", matchIfMissing = true)
public class LocalPromotionAssetStorage implements PromotionAssetStorage {

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Map<String, FileRule> RULES = Map.of(
            "image/jpeg", new FileRule(".jpg", LocalPromotionAssetStorage::isJpeg),
            "image/png", new FileRule(".png", LocalPromotionAssetStorage::isPng),
            "image/webp", new FileRule(".webp", LocalPromotionAssetStorage::isWebp));

    private final Path uploadRoot;

    public LocalPromotionAssetStorage(
            @Value("${promotion.assets.local-directory:./uploads/promotion-campaigns}")
            String uploadDirectory) {
        uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public StoredAsset storeCover(MultipartFile file) {
        ValidatedFile validated = validate(file);
        String storageKey = UUID.randomUUID() + validated.rule().extension();
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

    private ValidatedFile validate(MultipartFile file) {
        String message = "Campaign image must be JPEG, PNG, or WebP and no larger than 8 MB";
        if (file == null || file.isEmpty()) {
            throw new BusinessException("PROMOTION_ASSET_INVALID", message);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("PROMOTION_ASSET_TOO_LARGE", message);
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        FileRule rule = RULES.get(contentType);
        if (rule == null) {
            throw new BusinessException("PROMOTION_ASSET_INVALID", message);
        }
        try {
            byte[] signature = file.getInputStream().readNBytes(16);
            if (!rule.signature().test(signature)) {
                throw new BusinessException(
                        "PROMOTION_ASSET_INVALID",
                        "Campaign image content does not match its declared type");
            }
            return new ValidatedFile(contentType, rule);
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_INVALID", "Unable to read campaign image");
        }
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

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xff
                && bytes[1] == (byte) 0xd8
                && bytes[2] == (byte) 0xff;
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a;
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private record FileRule(String extension, Predicate<byte[]> signature) {}
    private record ValidatedFile(String contentType, FileRule rule) {}
}
