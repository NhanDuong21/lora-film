package com.lorafilm.movie.auditorium.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ScreenTypeConverter implements AttributeConverter<ScreenType, String> {

    @Override
    public String convertToDatabaseColumn(ScreenType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public ScreenType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return ScreenType.fromValue(dbData);
    }
}
