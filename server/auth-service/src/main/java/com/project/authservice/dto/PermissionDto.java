package com.project.authservice.dto;


public class PermissionDto {
    private Integer id;
    private String permissionCode;
    private String description;
    public Integer getId() {
        return this.id;
    }
    public String getPermissionCode() {
        return this.permissionCode;
    }
    public String getDescription() {
        return this.description;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public PermissionDto() {
    }
    public PermissionDto(Integer id, String permissionCode, String description) {
        this.id = id;
        this.permissionCode = permissionCode;
        this.description = description;
    }
    public static PermissionDtoBuilder builder() {
        return new PermissionDtoBuilder();
    }
    public static class PermissionDtoBuilder {
        private Integer id;
        private String permissionCode;
        private String description;
        PermissionDtoBuilder() {}
        public PermissionDtoBuilder id(Integer id) {
            this.id = id;
            return this;
        }
        public PermissionDtoBuilder permissionCode(String permissionCode) {
            this.permissionCode = permissionCode;
            return this;
        }
        public PermissionDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        public PermissionDto build() {
            return new PermissionDto(this.id, this.permissionCode, this.description);
        }
    }
}
