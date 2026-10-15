package com.bgsistemas.api.export;

import java.util.List;

// What every export needs, whatever the file type: a title, the column names and the rows (BGSI-252)
public record ExportTable(String title, List<String> headers, List<List<String>> rows) {
}
