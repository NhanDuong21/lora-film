package com.project.promotionservice.partner.dto.response;

import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.enums.PartnerType;
import com.project.promotionservice.partner.enums.SettlementCycle;

import java.time.Instant;

public class PartnerResponse {
    private String publicId;
    private String code;
    private String name;
    private PartnerType partnerType;
    private PartnerStatus status;
    private String taxCode;
    private String email;
    private String phone;
    private String contactPerson;
    private String address;
    private String website;
    private String contractNumber;
    private Instant contractStartAt;
    private Instant contractEndAt;
    private SettlementCycle settlementCycle;
    private String metadataJson;
    private Integer version;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(PartnerType partnerType) { this.partnerType = partnerType; }
    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
    public Instant getContractStartAt() { return contractStartAt; }
    public void setContractStartAt(Instant contractStartAt) { this.contractStartAt = contractStartAt; }
    public Instant getContractEndAt() { return contractEndAt; }
    public void setContractEndAt(Instant contractEndAt) { this.contractEndAt = contractEndAt; }
    public SettlementCycle getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(SettlementCycle settlementCycle) { this.settlementCycle = settlementCycle; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
