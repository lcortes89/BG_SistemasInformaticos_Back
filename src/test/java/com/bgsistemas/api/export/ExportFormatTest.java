package com.bgsistemas.api.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportFormatTest {

    @Test
    void shouldReadTheFormatWithoutCaringAboutCapitals() {
        assertThat(ExportFormat.from("pdf")).isEqualTo(ExportFormat.PDF);
        assertThat(ExportFormat.from(" XLSX ")).isEqualTo(ExportFormat.XLSX);
    }

    @Test
    void shouldRejectAnUnknownFormat() {
        assertThatThrownBy(() -> ExportFormat.from("docx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Format must be pdf or xlsx");
        assertThatThrownBy(() -> ExportFormat.from(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
