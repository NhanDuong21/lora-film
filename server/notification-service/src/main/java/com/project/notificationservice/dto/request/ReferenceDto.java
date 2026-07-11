package com.project.notificationservice.dto.request;

import jakarta.validation.constraints.Size;

public class ReferenceDto {

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    private String type;

    @Size(max = 100, message = "Reference id must not exceed 100 characters")
    private String id;

    public ReferenceDto() {
    }

    public ReferenceDto(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
