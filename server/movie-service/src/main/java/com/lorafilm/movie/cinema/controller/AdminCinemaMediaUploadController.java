package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.dto.MediaUploadResponse;
import com.lorafilm.movie.cinema.service.CloudinaryService;
import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/cinemas/media")
public class AdminCinemaMediaUploadController {

    private final CloudinaryService cloudinaryService;

    @Autowired
    public AdminCinemaMediaUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "cinemaPublicId", required = false) String cinemaPublicId) {

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid content type");
        }
        
        // Allowed types: LOGO, BANNER, GALLERY, MAP
        if (!type.equals("LOGO") && !type.equals("BANNER") && !type.equals("GALLERY") && !type.equals("MAP")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid media type");
        }
        
        // 10MB limit is usually handled by Spring config: spring.servlet.multipart.max-file-size
        // We'll trust the framework to reject oversized files based on global config

        MediaUploadResponse response = cloudinaryService.uploadImage(file, type, cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
