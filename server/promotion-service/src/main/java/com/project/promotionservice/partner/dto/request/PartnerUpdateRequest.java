package com.project.promotionservice.partner.dto.request;

import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.enums.PartnerType;
import com.project.promotionservice.partner.enums.SettlementCycle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PartnerUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private PartnerType partnerType;

    @Size(max = 50)
    private String taxCode;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String contactPerson;

    @Size(max = 500)
    private String address;

    @Size(max = 255)
    private String website;

    @Size(max = 100)
    private String contractNumber;

    private java.time.Instant contractStartAt;
    private java.time.Instant contractEndAt;

    @NotNull
    private SettlementCycle settlementCycle;

    private PartnerStatus status;

    @Size(max = 10000)
    private String metadataJson;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(PartnerType partnerType) { this.partnerType = partnerType; }
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
    public java.time.Instant getContractStartAt() { return contractStartAt; }
    public void setContractStartAt(java.time.Instant contractStartAt) { this.contractStartAt = contractStartAt; }
    public java.time.Instant getContractEndAt() { return contractEndAt; }
    public void setContractEndAt(java.time.Instant contractEndAt) { this.contractEndAt = contractEndAt; }
    public SettlementCycle getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(SettlementCycle settlementCycle) { this.settlementCycle = settlementCycle; }
    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
