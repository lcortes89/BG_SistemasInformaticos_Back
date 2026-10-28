package com.bgsistemas.api.export;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-255: the PDF is opened again to check its pages and its text
class PdfExporterTest {

    private final PdfExporter exporter = new PdfExporter();

    @Test
    void shouldCreateAPdfWithTheTitleAndTheRecords() throws Exception {
        ExportTable table = new ExportTable("Clientes", List.of("Nombre", "Estado"),
                List.of(List.of("Restaurante El Puerto", "Publicado")));

        byte[] file = exporter.export(table);

        assertThat(new String(file, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(file);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        assertThat(text).contains("Clientes").contains("Restaurante El Puerto").contains("BG Sistemas");
        reader.close();
    }

    @Test
    void shouldAddMorePagesWhenThereAreManyRecords() throws Exception {
        List<List<String>> rows = java.util.stream.IntStream.range(0, 120)
                .mapToObj(number -> List.of("Cliente " + number, "Publicado"))
                .toList();

        PdfReader reader = new PdfReader(exporter.export(new ExportTable("Clientes", List.of("Nombre", "Estado"), rows)));

        assertThat(reader.getNumberOfPages()).isGreaterThan(1);
        reader.close();
    }
}
