package com.lorafilm.movie.cinema.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lorafilm.movie.cinema.dto.MediaUploadResponse;
import com.lorafilm.movie.cinema.service.CloudinaryService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final String baseFolder;

    public CloudinaryServiceImpl(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:lorafilm/cinemas}") String baseFolder) {
        
        this.baseFolder = baseFolder;
        
        if (cloudName != null && !cloudName.isEmpty() 
            && apiKey != null && !apiKey.isEmpty() 
            && apiSecret != null && !apiSecret.isEmpty()) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));
        } else {
            log.warn("Cloudinary is not configured properly. Media upload will fail.");
            this.cloudinary = null;
        }
    }

    @Override
    public MediaUploadResponse uploadImage(MultipartFile file, String type, String cinemaPublicId) {
        if (cloudinary == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to upload file to Cloudinary");
        }
        
        try {
            String folder = baseFolder;
            if (cinemaPublicId != null && !cinemaPublicId.isEmpty()) {
                folder += "/" + cinemaPublicId;
            }
            if (type != null && !type.isEmpty()) {
                folder += "/" + type.toLowerCase();
            }

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            );

            // Do not use specific publicId unless necessary, letting Cloudinary generate it is safer for gallery
            // But we can append UUID to ensure uniqueness
            uploadParams.put("public_id", UUID.randomUUID().toString());

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            return MediaUploadResponse.builder()
                    .publicId((String) uploadResult.get("public_id"))
                    .secureUrl((String) uploadResult.get("secure_url"))
                    .width((Integer) uploadResult.get("width"))
                    .height((Integer) uploadResult.get("height"))
                    .format((String) uploadResult.get("format"))
                    .bytes(((Number) uploadResult.get("bytes")).longValue())
                    .resourceType((String) uploadResult.get("resource_type"))
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid image format. Supported formats: JPG, PNG, WEBP");
        }
    }
}
