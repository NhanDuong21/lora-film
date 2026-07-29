package com.project.userservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.userservice.config.CloudinaryProperties;
import com.project.userservice.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CloudinaryStorageService implements FileStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudinaryStorageService.class);
    private static final long AVATAR_MAX_BYTES = 5L * 1024 * 1024;
    private static final long DOCUMENT_MAX_BYTES = 10L * 1024 * 1024;
    private static final String AVATAR_CATEGORY = "avatars";
    private static final String DOCUMENT_CATEGORY = "employee-documents";

    private static final Map<String, FileRule> IMAGE_RULES = Map.of(
            "image/jpeg", new FileRule(".jpg", CloudinaryStorageService::isJpeg),
            "image/png", new FileRule(".png", CloudinaryStorageService::isPng),
            "image/webp", new FileRule(".webp", CloudinaryStorageService::isWebp));

    private static final Map<String, FileRule> DOCUMENT_RULES = Map.of(
            "application/pdf", new FileRule(".pdf", CloudinaryStorageService::isPdf),
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    new FileRule(".docx", CloudinaryStorageService::isZip),
            "image/jpeg", new FileRule(".jpg", CloudinaryStorageService::isJpeg),
            "image/png", new FileRule(".png", CloudinaryStorageService::isPng));

    private final Cloudinary cloudinary;
    private final String assetFolder;
    private final String publicIdPrefix;

    public CloudinaryStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.assetFolder = normalizeFolder(properties.getFolder());
        this.publicIdPrefix = assetFolder.replace('/', '_').replaceAll("[^A-Za-z0-9_-]", "_");
    }

    @Override
    public StoredFile storeAvatar(MultipartFile file) {
        ValidatedFile validated = validate(file, AVATAR_MAX_BYTES, IMAGE_RULES,
                "Avatar must be a JPEG, PNG, or WebP image up to 5 MB");
        return upload(file, validated, AVATAR_CATEGORY, "image", "upload", false);
    }

    @Override
    public StoredFile storeEmployeeDocument(MultipartFile file) {
        ValidatedFile validated = validate(file, DOCUMENT_MAX_BYTES, DOCUMENT_RULES,
                "Document must be a PDF, DOCX, JPEG, or PNG file up to 10 MB");
        return upload(file, validated, DOCUMENT_CATEGORY, "raw", "authenticated", true);
    }

    @Override
    public Resource load(String category, String publicId) {
        validateCategory(category);
        validatePublicId(publicId);
        String resourceType = AVATAR_CATEGORY.equals(category) ? "image" : "raw";
        String deliveryType = AVATAR_CATEGORY.equals(category) ? "upload" : "authenticated";
        try {
            String url = cloudinary.url()
                    .secure(true)
                    .signed(DOCUMENT_CATEGORY.equals(category))
                    .resourceType(resourceType)
                    .type(deliveryType)
                    .generate(publicId);
            return new UrlResource(url);
        } catch (MalformedURLException | RuntimeException exception) {
            throw storageFailure("Unable to load file", exception);
        }
    }

    @Override
    public void delete(String category, String publicId) {
        validateCategory(category);
        validatePublicId(publicId);
        String resourceType = AVATAR_CATEGORY.equals(category) ? "image" : "raw";
        String deliveryType = AVATAR_CATEGORY.equals(category) ? "upload" : "authenticated";
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", deliveryType,
                    "invalidate", true));
            Object status = result.get("result");
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new IOException("Unexpected Cloudinary delete result");
            }
        } catch (IOException | RuntimeException exception) {
            throw storageFailure("Unable to delete file", exception);
        }
    }

    private StoredFile upload(MultipartFile file, ValidatedFile validated, String category,
                              String resourceType, String deliveryType, boolean includeExtension) {
        String generatedId = publicIdPrefix + "_" + category.replace('-', '_')
                + "_" + UUID.randomUUID();
        String requestedPublicId = includeExtension
                ? generatedId + validated.rule().extension()
                : generatedId;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", requestedPublicId,
                    "asset_folder", assetFolder + "/" + category,
                    "resource_type", resourceType,
                    "type", deliveryType,
                    "overwrite", false,
                    "unique_filename", false,
                    "use_filename", false));
            String publicId = requiredResult(result, "public_id");
            String secureUrl = requiredResult(result, "secure_url");
            return new StoredFile(publicId, secureUrl, validated.contentType(), file.getSize());
        } catch (IOException | RuntimeException exception) {
            throw storageFailure("Unable to upload file", exception);
        }
    }

    private ValidatedFile validate(MultipartFile file, long maxBytes,
                                   Map<String, FileRule> rules, String validationMessage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(validationMessage, "USER_FILE_INVALID");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(validationMessage, "USER_FILE_TOO_LARGE");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        FileRule rule = rules.get(contentType);
        if (rule == null) {
            throw new BusinessException(validationMessage, "USER_FILE_INVALID");
        }
        try {
            byte[] signature = file.getInputStream().readNBytes(16);
            if (!rule.signature().test(signature)) {
                throw new BusinessException("File content does not match its declared type",
                        "USER_FILE_INVALID");
            }
            if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    .equals(contentType) && !isDocx(file)) {
                throw new BusinessException("DOCX package is missing required document entries",
                        "USER_FILE_INVALID");
            }
            return new ValidatedFile(contentType, rule);
        } catch (IOException exception) {
            throw new BusinessException("Unable to read uploaded file", "USER_FILE_INVALID");
        }
    }

    private String requiredResult(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Cloudinary response is missing " + key);
        }
        return text;
    }

    private void validateCategory(String category) {
        if (!AVATAR_CATEGORY.equals(category) && !DOCUMENT_CATEGORY.equals(category)) {
            throw new BusinessException("Invalid storage category", "USER_FILE_INVALID");
        }
    }

    private void validatePublicId(String publicId) {
        if (publicId == null || publicId.isBlank() || publicId.length() > 255
                || publicId.contains("..") || publicId.contains("\\")
                || publicId.contains("?") || publicId.contains("#")) {
            throw new BusinessException("Invalid storage identifier", "USER_FILE_INVALID");
        }
    }

    private BusinessException storageFailure(String message, Exception exception) {
        LOGGER.error("{} in Cloudinary: {}", message, exception.getClass().getSimpleName());
        return new BusinessException(message, "USER_STORAGE_UNAVAILABLE");
    }

    private static String normalizeFolder(String folder) {
        String normalized = folder == null ? "" : folder.trim().replace('\\', '/');
        normalized = normalized.replaceAll("^/+", "").replaceAll("/+$", "");
        if (normalized.isBlank() || normalized.contains("..")
                || !normalized.matches("[A-Za-z0-9_/-]+")) {
            throw new IllegalArgumentException("Cloudinary folder is invalid");
        }
        return normalized;
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
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47
                && bytes[4] == 0x0d
                && bytes[5] == 0x0a
                && bytes[6] == 0x1a
                && bytes[7] == 0x0a;
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static boolean isPdf(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    private static boolean isZip(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 'P' && bytes[1] == 'K'
                && ((bytes[2] == 3 && bytes[3] == 4)
                || (bytes[2] == 5 && bytes[3] == 6)
                || (bytes[2] == 7 && bytes[3] == 8));
    }

    private static boolean isDocx(MultipartFile file) {
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && entryCount++ < 200) {
                String name = entry.getName();
                hasContentTypes |= "[Content_Types].xml".equals(name);
                hasDocument |= "word/document.xml".equals(name);
                if (hasContentTypes && hasDocument) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            return false;
        }
    }

    private record FileRule(String extension, Predicate<byte[]> signature) {
    }

    private record ValidatedFile(String contentType, FileRule rule) {
    }
}
