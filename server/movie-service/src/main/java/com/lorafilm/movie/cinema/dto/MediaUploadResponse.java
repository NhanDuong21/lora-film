package com.lorafilm.movie.cinema.dto;

public class MediaUploadResponse {
    private String publicId;
    private String secureUrl;
    private Integer width;
    private Integer height;
    private String format;
    private Long bytes;
    private String resourceType;

    public MediaUploadResponse() {
    }

    public MediaUploadResponse(String publicId, String secureUrl, Integer width, Integer height, String format, Long bytes, String resourceType) {
        this.publicId = publicId;
        this.secureUrl = secureUrl;
        this.width = width;
        this.height = height;
        this.format = format;
        this.bytes = bytes;
        this.resourceType = resourceType;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public void setSecureUrl(String secureUrl) {
        this.secureUrl = secureUrl;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public static MediaUploadResponseBuilder builder() {
        return new MediaUploadResponseBuilder();
    }

    public static class MediaUploadResponseBuilder {
        private String publicId;
        private String secureUrl;
        private Integer width;
        private Integer height;
        private String format;
        private Long bytes;
        private String resourceType;

        MediaUploadResponseBuilder() {
        }

        public MediaUploadResponseBuilder publicId(String publicId) {
            this.publicId = publicId;
            return this;
        }

        public MediaUploadResponseBuilder secureUrl(String secureUrl) {
            this.secureUrl = secureUrl;
            return this;
        }

        public MediaUploadResponseBuilder width(Integer width) {
            this.width = width;
            return this;
        }

        public MediaUploadResponseBuilder height(Integer height) {
            this.height = height;
            return this;
        }

        public MediaUploadResponseBuilder format(String format) {
            this.format = format;
            return this;
        }

        public MediaUploadResponseBuilder bytes(Long bytes) {
            this.bytes = bytes;
            return this;
        }

        public MediaUploadResponseBuilder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public MediaUploadResponse build() {
            return new MediaUploadResponse(publicId, secureUrl, width, height, format, bytes, resourceType);
        }
    }
}
