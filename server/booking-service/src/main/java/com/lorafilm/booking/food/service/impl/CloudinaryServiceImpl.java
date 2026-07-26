package com.lorafilm.booking.food.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.food.dto.response.MediaUploadResponse;
import com.lorafilm.booking.food.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

    private final Cloudinary cloudinary;
    private final String baseFolder;

    public CloudinaryServiceImpl(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:lorafilm/foods}") String baseFolder) {
        
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
    public MediaUploadResponse uploadImage(MultipartFile file, String type, String publicId) {
        if (cloudinary == null) {
            throw new BusinessException("INTERNAL_SERVER_ERROR", "Failed to upload file to Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        try {
            String folder = baseFolder;
            if (publicId != null && !publicId.isEmpty()) {
                folder += "/" + publicId;
            }
            if (type != null && !type.isEmpty()) {
                folder += "/" + type.toLowerCase();
            }

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            );

            uploadParams.put("public_id", UUID.randomUUID().toString());

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            return new MediaUploadResponse(
                    (String) uploadResult.get("public_id"),
                    (String) uploadResult.get("secure_url"),
                    (Integer) uploadResult.get("width"),
                    (Integer) uploadResult.get("height"),
                    (String) uploadResult.get("format"),
                    ((Number) uploadResult.get("bytes")).longValue(),
                    (String) uploadResult.get("resource_type")
            );

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BusinessException("VALIDATION_ERROR", "Invalid image format. Supported formats: JPG, PNG, WEBP", HttpStatus.BAD_REQUEST);
        }
    }
}
