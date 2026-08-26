package com.project.promotionservice.presentation.storage;

import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

final class PromotionAssetFileValidator {

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Map<String, FileRule> RULES = Map.of(
            "image/jpeg", new FileRule(".jpg", PromotionAssetFileValidator::isJpeg),
            "image/png", new FileRule(".png", PromotionAssetFileValidator::isPng),
            "image/webp", new FileRule(".webp", PromotionAssetFileValidator::isWebp));

    private PromotionAssetFileValidator() {
    }

    static ValidatedFile validate(MultipartFile file) {
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
            return new ValidatedFile(contentType, rule.extension());
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROMOTION_ASSET_INVALID", "Unable to read campaign image");
        }
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

    private record FileRule(String extension, Predicate<byte[]> signature) {
    }

    record ValidatedFile(String contentType, String extension) {
    }
}
