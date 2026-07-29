package com.project.promotionservice.benefit.service;

import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class VerifiedPhoneNormalizer {

    public String normalizeNullable(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String compact = rawPhone.trim().replaceAll("[\\s().-]", "");
        if (compact.startsWith("00")) {
            compact = "+" + compact.substring(2);
        } else if (compact.matches("^0\\d{9}$")) {
            compact = "+84" + compact.substring(1);
        } else if (compact.matches("^84\\d{9}$")) {
            compact = "+" + compact;
        }
        if (!compact.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new BusinessException(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "customerPhone must be a verified E.164 phone number",
                    HttpStatus.BAD_REQUEST);
        }
        return compact;
    }
}
