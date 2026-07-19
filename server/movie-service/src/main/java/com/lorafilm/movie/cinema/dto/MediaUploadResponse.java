package com.lorafilm.movie.cinema.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaUploadResponse {
    private String publicId;
    private String secureUrl;
    private Integer width;
    private Integer height;
    private String format;
    private Long bytes;
    private String resourceType;
}
