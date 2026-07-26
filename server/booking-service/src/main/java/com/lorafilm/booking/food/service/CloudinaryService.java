package com.lorafilm.booking.food.service;

import com.lorafilm.booking.food.dto.response.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    MediaUploadResponse uploadImage(MultipartFile file, String type, String publicId);
}
