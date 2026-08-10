package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    MediaUploadResponse uploadImage(MultipartFile file, String type, String cinemaPublicId);
}
