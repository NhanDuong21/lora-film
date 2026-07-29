package com.project.promotionservice.partner.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.enums.PartnerType;
import com.project.promotionservice.partner.enums.SettlementCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "partners")
public class Partner extends BaseAuditableEntity {

    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", length = 50, nullable = false)
    private PartnerType partnerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PartnerStatus status = PartnerStatus.ACTIVE;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "contract_number", length = 100)
    private String contractNumber;

    @Column(name = "contract_start_at")
    private Instant contractStartAt;

    @Column(name = "contract_end_at")
    private Instant contractEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_cycle", length = 30, nullable = false)
    private SettlementCycle settlementCycle;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PartnerType getPartnerType() {
        return partnerType;
    }

    public void setPartnerType(PartnerType partnerType) {
        this.partnerType = partnerType;
    }

    public PartnerStatus getStatus() {
        return status;
    }

    public void setStatus(PartnerStatus status) {
        this.status = status;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public Instant getContractStartAt() {
        return contractStartAt;
    }

    public void setContractStartAt(Instant contractStartAt) {
        this.contractStartAt = contractStartAt;
    }

    public Instant getContractEndAt() {
        return contractEndAt;
    }

    public void setContractEndAt(Instant contractEndAt) {
        this.contractEndAt = contractEndAt;
    }

    public SettlementCycle getSettlementCycle() {
        return settlementCycle;
    }

    public void setSettlementCycle(SettlementCycle settlementCycle) {
        this.settlementCycle = settlementCycle;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
