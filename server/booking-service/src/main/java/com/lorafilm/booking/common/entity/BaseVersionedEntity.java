package com.lorafilm.booking.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseVersionedEntity extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @Version
    @Column(name = "version")
    private Integer version;

    public BaseVersionedEntity() {
    }

    public BaseVersionedEntity(Long id, Instant createdAt, String publicId, Integer version) {
        super(id, createdAt);
        this.publicId = publicId;
        this.version = version;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public void setPublicId(UUID uuid) {
        this.publicId = uuid != null ? uuid.toString() : null;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
