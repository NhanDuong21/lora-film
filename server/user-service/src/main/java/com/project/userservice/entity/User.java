package com.project.userservice.entity;

import com.project.userservice.enumtype.Gender;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.enumtype.AccountType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.project.userservice.security.EncryptedStringConverter;
import com.project.userservice.security.PiiCrypto;

@Entity
@Table(name = "users")
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class User {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_number", length = 512)
    private String phoneNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "cccd", length = 512)
    private String cccd;

    @Column(name = "phone_hash", unique = true, length = 64, columnDefinition = "CHAR(64)")
    private String phoneHash;

    @Column(name = "cccd_hash", unique = true, length = 64, columnDefinition = "CHAR(64)")
    private String cccdHash;

    @Column(name = "cccd_masked", length = 20)
    private String cccdMasked;

    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(name = "province_name", length = 150)
    private String provinceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType = AccountType.CUSTOMER;

    @Column(name = "pii_key_version", nullable = false)
    private Integer piiKeyVersion = 1;

    @Column(name = "pii_retention_until")
    private LocalDate piiRetentionUntil;

    @Column(name = "pii_erased_at")
    private LocalDateTime piiErasedAt;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "is_test_account", nullable = false)
    private Boolean testAccount = false;

    @org.springframework.data.annotation.CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @org.springframework.data.annotation.LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(Long accountId, String fullName, String phoneNumber, String cccd, String cccdMasked,
                String provinceCode, String provinceName, Gender gender, LocalDate birthday,
                Integer birthYear, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.cccd = cccd;
        this.cccdMasked = cccdMasked;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.gender = gender;
        this.birthday = birthday;
        this.birthYear = birthYear;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.phoneHash = PiiCrypto.searchHash(phoneNumber);
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
        this.cccdHash = PiiCrypto.searchHash(cccd);
    }

    public String getCccdMasked() {
        return cccdMasked;
    }

    public void setCccdMasked(String cccdMasked) {
        this.cccdMasked = cccdMasked;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getPhoneHash() { return phoneHash; }
    public String getCccdHash() { return cccdHash; }
    public Integer getPiiKeyVersion() { return piiKeyVersion; }
    public void setPiiKeyVersion(Integer piiKeyVersion) { this.piiKeyVersion = piiKeyVersion; }
    public LocalDate getPiiRetentionUntil() { return piiRetentionUntil; }
    public void setPiiRetentionUntil(LocalDate piiRetentionUntil) { this.piiRetentionUntil = piiRetentionUntil; }
    public LocalDateTime getPiiErasedAt() { return piiErasedAt; }
    public void setPiiErasedAt(LocalDateTime piiErasedAt) { this.piiErasedAt = piiErasedAt; }
    public void refreshPiiProtection() {
        this.phoneHash = PiiCrypto.searchHash(phoneNumber);
        this.cccdHash = PiiCrypto.searchHash(cccd);
        this.piiKeyVersion = 1;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Boolean getTestAccount() {
        return testAccount;
    }

    public void setTestAccount(Boolean testAccount) {
        this.testAccount = testAccount;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
