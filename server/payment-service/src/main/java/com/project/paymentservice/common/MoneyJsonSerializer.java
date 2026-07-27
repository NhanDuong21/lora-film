package com.project.paymentservice.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Keeps monetary JSON stable across the first response and a replay restored
 * from MySQL JSON, whose binary representation normalizes insignificant zeros.
 */
public class MoneyJsonSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(
            BigDecimal value,
            JsonGenerator generator,
            SerializerProvider serializers) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        generator.writeNumber(value.setScale(2, RoundingMode.UNNECESSARY));
    }
}
