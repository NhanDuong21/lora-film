package com.lorafilm.movie.movie.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MovieFormatConverter implements AttributeConverter<MovieFormat, String> {

    @Override
    public String convertToDatabaseColumn(MovieFormat attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public MovieFormat convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return MovieFormat.fromValue(dbData);
    }
}
