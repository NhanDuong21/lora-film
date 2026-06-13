package com.project.userservice.entity;

import com.project.userservice.enumtype.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "cccd", unique = true, length = 12)
    private String cccd;

    @Column(name = "cccd_masked", length = 20)
    private String cccdMasked;

    @Column(name = "province_code", length = 10)
    private String provinceCode;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "is_verified_phone")
    @Builder.Default
    private Boolean isVerifiedPhone = false;

    @Column(name = "cccd_checked_at")
    private LocalDateTime cccdCheckedAt;

    @Column(name = "cccd_check_note")
    private String cccdCheckNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
