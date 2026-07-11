package com.project.promotionservice.util;

public class PromotionCodeNormalizer {
    public static String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toUpperCase();
    }
}
