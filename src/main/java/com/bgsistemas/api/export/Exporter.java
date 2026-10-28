package com.bgsistemas.api.export;

// One implementation per file type: PdfExporter and ExcelExporter (BGSI-252)
public interface Exporter {

    ExportFormat format();

    byte[] export(ExportTable table);
}
