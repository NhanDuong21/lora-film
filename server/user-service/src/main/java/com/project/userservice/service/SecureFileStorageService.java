package com.project.userservice.service;

import com.project.userservice.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SecureFileStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureFileStorageService.class);
    private static final long AVATAR_MAX_BYTES = 5L * 1024 * 1024;
    private static final long DOCUMENT_MAX_BYTES = 10L * 1024 * 1024;

    private static final Map<String, FileRule> IMAGE_RULES = Map.of(
            "image/jpeg", new FileRule(".jpg", SecureFileStorageService::isJpeg),
            "image/png", new FileRule(".png", SecureFileStorageService::isPng),
            "image/webp", new FileRule(".webp", SecureFileStorageService::isWebp));

    private static final Map<String, FileRule> DOCUMENT_RULES = Map.of(
            "application/pdf", new FileRule(".pdf", SecureFileStorageService::isPdf),
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    new FileRule(".docx", SecureFileStorageService::isZip),
            "image/jpeg", new FileRule(".jpg", SecureFileStorageService::isJpeg),
            "image/png", new FileRule(".png", SecureFileStorageService::isPng));

    private final Path uploadRoot;

    public SecureFileStorageService(@Value("${app.upload.directory:./uploads}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public StoredFile storeAvatar(MultipartFile file) {
        return store(file, "avatars", AVATAR_MAX_BYTES, IMAGE_RULES,
                "Avatar must be a JPEG, PNG, or WebP image up to 5 MB");
    }

    public StoredFile storeEmployeeDocument(MultipartFile file) {
        return store(file, "employee-documents", DOCUMENT_MAX_BYTES, DOCUMENT_RULES,
                "Document must be a PDF, DOCX, JPEG, or PNG file up to 10 MB");
    }

    public Resource load(String directory, String fileName) {
        try {
            Resource resource = new UrlResource(safePath(directory, fileName).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("File not found", "USER_FILE_NOT_FOUND");
            }
            return resource;
        } catch (java.net.MalformedURLException exception) {
            throw new BusinessException("File not found", "USER_FILE_NOT_FOUND");
        }
    }

    public void delete(String directory, String fileName) {
        try {
            Files.deleteIfExists(safePath(directory, fileName));
        } catch (IOException exception) {
            throw new BusinessException("Unable to delete file", "USER_UPLOAD_FAILED");
        }
    }

    public void deleteAfterCommit(String directory, String fileName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(directory, fileName);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    delete(directory, fileName);
                } catch (RuntimeException exception) {
                    LOGGER.error("Unable to remove committed file {}/{}", directory, fileName, exception);
                }
            }
        });
    }

    private StoredFile store(MultipartFile file, String directory, long maxBytes,
                             Map<String, FileRule> rules, String validationMessage) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) {
            throw new BusinessException(validationMessage, "USER_FILE_INVALID");
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
                throw new BusinessException("File content does not match its declared type", "USER_FILE_INVALID");
            }
            if (contentType.equals(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    && !isDocx(file)) {
                throw new BusinessException("DOCX package is missing required document entries",
                        "USER_FILE_INVALID");
            }
            String storedName = UUID.randomUUID() + rule.extension();
            Path target = safePath(directory, storedName);
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target);
            return new StoredFile(storedName, contentType, file.getSize());
        } catch (IOException exception) {
            throw new BusinessException("Unable to store file", "USER_UPLOAD_FAILED");
        }
    }

    private Path safePath(String directory, String fileName) {
        Path directoryPath = uploadRoot.resolve(directory).normalize();
        if (!directoryPath.startsWith(uploadRoot)) {
            throw new BusinessException("Invalid file path", "USER_FILE_INVALID");
        }
        Path target = directoryPath.resolve(fileName).normalize();
        if (!target.startsWith(directoryPath) || !target.getFileName().toString().equals(fileName)) {
            throw new BusinessException("Invalid file path", "USER_FILE_INVALID");
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

    public record StoredFile(String fileName, String contentType, long fileSize) {
    }
}
