package com.bgsistemas.api.export;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

// Builds an .xlsx file with Apache POI: one header row + one row per record (BGSI-252)
@Component
public class ExcelExporter implements Exporter {

    @Override
    public ExportFormat format() {
        return ExportFormat.XLSX;
    }

    @Override
    public byte[] export(ExportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(table.title());

            Row header = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int column = 0; column < table.headers().size(); column++) {
                header.createCell(column).setCellValue(table.headers().get(column));
                header.getCell(column).setCellStyle(headerStyle);
            }

            for (int index = 0; index < table.rows().size(); index++) {
                Row row = sheet.createRow(index + 1);
                List<String> values = table.rows().get(index);
                for (int column = 0; column < values.size(); column++) {
                    row.createCell(column).setCellValue(values.get(column) == null ? "" : values.get(column));
                }
            }

            // Filters on the header and columns wide enough for their text
            if (!table.rows().isEmpty()) {
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, table.rows().size(), 0,
                        table.headers().size() - 1));
            }
            for (int column = 0; column < table.headers().size(); column++) {
                sheet.autoSizeColumn(column);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("The Excel file could not be created", e);
        }
    }

    // Navy background and white bold text, like the tables of the website
    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
