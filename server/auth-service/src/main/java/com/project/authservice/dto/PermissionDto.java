package com.project.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionDto {
    private Long id;

    @NotBlank(message = "Permission code is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,99}$", message = "Permission code must contain only uppercase letters, numbers, and underscores")
    private String code;

    @NotBlank(message = "Permission name is required")
    @Size(max = 150, message = "Permission name must not exceed 150 characters")
    private String name;

    @Size(max = 100, message = "Module must not exceed 100 characters")
    private String module;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public PermissionDto() {
    }

    public PermissionDto(Long id, String code, String name, String module, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.module = module;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonIgnore
    public String getPermissionCode() {
        return code;
    }

    @JsonIgnore
    public void setPermissionCode(String permissionCode) {
        this.code = permissionCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static PermissionDtoBuilder builder() {
        return new PermissionDtoBuilder();
    }

    public static class PermissionDtoBuilder {
        private Long id;
        private String code;
        private String name;
        private String module;
        private String description;

        public PermissionDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PermissionDtoBuilder code(String code) {
            this.code = code;
            return this;
        }

        public PermissionDtoBuilder permissionCode(String permissionCode) {
            this.code = permissionCode;
            return this;
        }

        public PermissionDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PermissionDtoBuilder module(String module) {
            this.module = module;
            return this;
        }

        public PermissionDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PermissionDto build() {
            return new PermissionDto(id, code, name, module, description);
        }
    }
}
