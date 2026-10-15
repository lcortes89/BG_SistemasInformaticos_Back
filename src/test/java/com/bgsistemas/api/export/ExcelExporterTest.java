package com.bgsistemas.api.export;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-255: the Excel file is opened again to check what it contains
class ExcelExporterTest {

    private final ExcelExporter exporter = new ExcelExporter();

    @Test
    void shouldWriteTheHeaderAndOneRowPerRecord() throws Exception {
        ExportTable table = new ExportTable("Clientes", List.of("Nombre", "Estado"),
                List.of(List.of("Café Central", "Publicado"), List.of("Hotel Avilés", "Oculto")));

        byte[] file = exporter.export(table);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheet("Clientes");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nombre");
            assertThat(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Café Central");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Oculto");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void shouldCreateAFileEvenWithoutRecords() throws Exception {
        byte[] file = exporter.export(new ExportTable("Marcas", List.of("Nombre"), List.of()));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertThat(workbook.getSheet("Marcas").getLastRowNum()).isZero();
        }
    }
}
