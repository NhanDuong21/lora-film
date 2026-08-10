package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.lorafilm.movie.movie.domain.enums.CreditRoleType;

public class MovieCreditRequest {
    @NotBlank(message = "Person public ID is required")
    private String personPublicId;

    @NotNull(message = "Role type is required")
    private CreditRoleType roleType;

    @jakarta.validation.constraints.Size(max = 150, message = "Character name cannot exceed 150 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]*$", message = "Character name contains invalid characters")
    private String characterName;

    @jakarta.validation.constraints.Min(value = 0, message = "Thứ tự hiển thị không được là số âm.")
    private Integer displayOrder = 0;

    public MovieCreditRequest() {}

    public String getPersonPublicId() { return personPublicId; }
    public void setPersonPublicId(String personPublicId) { this.personPublicId = personPublicId; }

    public CreditRoleType getRoleType() { return roleType; }
    public void setRoleType(CreditRoleType roleType) { this.roleType = roleType; }

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
