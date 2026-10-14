package com.bgsistemas.api.globals;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Saves a short list of texts in ONE column, one text per line (BGSI-205).
// E.g. ["Bares", "Restaurantes"] <-> "Bares\nRestaurantes". No extra tables needed.
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = "\n";

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(SEPARATOR, values);
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(column.split(SEPARATOR)));
    }
}
