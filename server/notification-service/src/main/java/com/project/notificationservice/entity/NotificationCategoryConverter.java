package com.project.notificationservice.entity;

import com.project.notificationservice.domain.NotificationTypes.Category;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Keeps notification requests readable after the category names were aligned
 * with the template registry. Older local data used BOOKING and PROMOTION.
 */
@Converter
public class NotificationCategoryConverter implements AttributeConverter<Category, String> {

    @Override
    public String convertToDatabaseColumn(Category category) {
        return category == null ? null : category.name();
    }

    @Override
    public Category convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value) {
            case "BOOKING" -> Category.TRANSACTIONAL;
            case "PROMOTION" -> Category.MARKETING;
            default -> Category.valueOf(value);
        };
    }
}
