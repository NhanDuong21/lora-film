package com.project.movieservice.converter;

import com.project.movieservice.enumtype.ScreenType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ScreenTypeConverter implements AttributeConverter<ScreenType, String> {

    @Override
    public String convertToDatabaseColumn(ScreenType screenType) {
        if (screenType == null) {
            return null;
        }
        return screenType == ScreenType._4DX ? "4DX" : screenType.name();
    }

    @Override
    public ScreenType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ScreenType.fromString(dbData);
    }
}
