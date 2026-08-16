package com.project.authservice.dto.response;

public class IdentityNumberInsightResponse {
    private final String identityNumberMasked;
    private final String birthRegistrationProvinceCode;
    private final String birthRegistrationProvinceName;
    private final String legalSexLabel;
    private final int birthYear;

    public IdentityNumberInsightResponse(
            String identityNumberMasked,
            String birthRegistrationProvinceCode,
            String birthRegistrationProvinceName,
            String legalSexLabel,
            int birthYear) {
        this.identityNumberMasked = identityNumberMasked;
        this.birthRegistrationProvinceCode = birthRegistrationProvinceCode;
        this.birthRegistrationProvinceName = birthRegistrationProvinceName;
        this.legalSexLabel = legalSexLabel;
        this.birthYear = birthYear;
    }

    public String getIdentityNumberMasked() {
        return identityNumberMasked;
    }

    public String getBirthRegistrationProvinceCode() {
        return birthRegistrationProvinceCode;
    }

    public String getBirthRegistrationProvinceName() {
        return birthRegistrationProvinceName;
    }

    public String getLegalSexLabel() {
        return legalSexLabel;
    }

    public int getBirthYear() {
        return birthYear;
    }
}
