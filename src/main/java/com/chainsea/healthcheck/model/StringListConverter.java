package com.chainsea.healthcheck.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converter for List<String> to PostgreSQL TEXT.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String COMMA = ",";

    @Override
    public String convertToDatabaseColumn(List<String> stringList) {
        if (stringList == null || stringList.isEmpty()) {
            return null;
        }
        return String.join(COMMA, stringList);
    }

    @Override
    public List<String> convertToEntityAttribute(String string) {
        if (string == null || string.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(string.split(COMMA)));
    }
}
