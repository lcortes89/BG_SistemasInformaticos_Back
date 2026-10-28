package com.bgsistemas.api.globals;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void shouldSaveTheListOneTextPerLine() {
        assertThat(converter.convertToDatabaseColumn(List.of("Bares", "Restaurantes"))).isEqualTo("Bares\nRestaurantes");
    }

    @Test
    void shouldReadTheColumnBackAsAList() {
        assertThat(converter.convertToEntityAttribute("Bares\nRestaurantes")).containsExactly("Bares", "Restaurantes");
    }

    @Test
    void shouldTreatEmptyValuesAsAnEmptyList() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
    }
}
