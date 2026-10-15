package com.bgsistemas.api.export;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Chooses the right exporter and turns the file into a download (BGSI-252)
@Component
public class ExportService {

    private final Map<ExportFormat, Exporter> exporters;

    // Spring gives us every Exporter bean (PdfExporter and ExcelExporter)
    public ExportService(List<Exporter> exporters) {
        this.exporters = exporters.stream().collect(Collectors.toMap(Exporter::format, Function.identity()));
    }

    // fileName "clientes" -> "clientes-2026-10-15.pdf"
    public ResponseEntity<byte[]> download(ExportTable table, ExportFormat format, String fileName) {
        byte[] file = exporters.get(format).export(table);
        String name = fileName + "-" + LocalDate.now() + "." + format.getExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .contentType(MediaType.parseMediaType(format.getContentType()))
                .contentLength(file.length)
                .body(file);
    }
}
