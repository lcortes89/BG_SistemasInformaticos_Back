package com.bgsistemas.api.export;

import java.util.Arrays;

// The two file types the admin can download (BGSI-252)
public enum ExportFormat {

    PDF("pdf", "application/pdf"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    ExportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() { return extension; }
    public String getContentType() { return contentType; }

    // "pdf", "PDF", "xlsx"... -> the format. Anything else -> 400 (IllegalArgumentException)
    public static ExportFormat from(String value) {
        return Arrays.stream(values())
                .filter(format -> format.extension.equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Format must be pdf or xlsx"));
    }
}
