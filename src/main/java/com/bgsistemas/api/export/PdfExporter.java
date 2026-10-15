package com.bgsistemas.api.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Builds the PDF with OpenPDF: logo of the header, title, date, orange line and the table (BGSI-252)
@Component
public class PdfExporter implements Exporter {

    private static final String LOGO = "pdf/logo.png";
    private static final Color NAVY = new Color(11, 31, 58);
    private static final Color ORANGE = new Color(245, 138, 31);
    private static final Color GREY = new Color(100, 116, 139);
    private static final Color STRIPE = new Color(245, 247, 250);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ExportFormat format() {
        return ExportFormat.PDF;
    }

    @Override
    public byte[] export(ExportTable table) {
        try {
            return buildPdf(table);
        } catch (Exception e) {
            throw new IllegalStateException("The PDF file could not be created", e);
        }
    }

    private byte[] buildPdf(ExportTable table) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 50);
        PdfWriter.getInstance(document, out);

        // "Página 1", "Página 2"... at the bottom of every page
        HeaderFooter footer = new HeaderFooter(new Phrase("Página ", new Font(Font.HELVETICA, 8, Font.NORMAL, GREY)), true);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setBorder(Rectangle.NO_BORDER);
        document.setFooter(footer);

        document.open();
        document.add(titleWithLogo(table.title()));
        document.add(new LineSeparator(2f, 100f, ORANGE, Element.ALIGN_CENTER, -4f));
        document.add(dataTable(table));

        Paragraph total = new Paragraph("Total: " + table.rows().size() + " registros",
                new Font(Font.HELVETICA, 8, Font.NORMAL, GREY));
        total.setAlignment(Element.ALIGN_CENTER);
        total.setSpacingBefore(12);
        document.add(total);

        document.close();
        return out.toByteArray();
    }

    // Left: title + "BG Sistemas Informáticos · Exportado el 15/10/2026". Right: the logo
    private PdfPTable titleWithLogo(String title) throws Exception {
        PdfPTable header = new PdfPTable(new float[] { 5f, 1f });
        header.setWidthPercentage(100);

        PdfPCell text = new PdfPCell();
        text.setBorder(Rectangle.NO_BORDER);
        text.addElement(new Paragraph(title, new Font(Font.HELVETICA, 18, Font.BOLD, NAVY)));
        text.addElement(new Paragraph("BG Sistemas Informáticos · Exportado el " + LocalDate.now().format(DATE),
                new Font(Font.HELVETICA, 9, Font.NORMAL, GREY)));
        header.addCell(text);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Image logo = loadLogo();
        if (logo != null) {
            logo.scaleToFit(40, 46);
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);
        header.setSpacingAfter(6);
        return header;
    }

    // Navy header and striped rows
    private PdfPTable dataTable(ExportTable table) {
        PdfPTable pdfTable = new PdfPTable(table.headers().size());
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(14);
        pdfTable.setHeaderRows(1); // the header is repeated on every page

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        for (String title : table.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(title, headerFont));
            cell.setBackgroundColor(NAVY);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(6);
            pdfTable.addCell(cell);
        }

        Font rowFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
        for (int index = 0; index < table.rows().size(); index++) {
            for (String value : table.rows().get(index)) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, rowFont));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(6);
                if (index % 2 == 1) cell.setBackgroundColor(STRIPE);
                pdfTable.addCell(cell);
            }
        }
        return pdfTable;
    }

    // If the logo is missing the PDF is still created, only without it
    private Image loadLogo() {
        try (InputStream in = new ClassPathResource(LOGO).getInputStream()) {
            return Image.getInstance(in.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }
}
