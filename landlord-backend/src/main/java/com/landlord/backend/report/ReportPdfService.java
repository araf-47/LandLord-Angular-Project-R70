package com.landlord.backend.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Extends the receipt PDF pattern from billing/ReceiptService.java - in-memory OpenPDF document, nothing persisted. */
@Service
public class ReportPdfService {

    public byte[] render(ReportTable table) {
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph(table.title(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(table.subtitle(), valueFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            PdfPTable pdfTable = new PdfPTable(table.headers().size());
            pdfTable.setWidthPercentage(100);

            for (String header : table.headers()) {
                addCell(pdfTable, header, headerFont);
            }
            for (List<String> row : table.rows()) {
                for (String value : row) {
                    addCell(pdfTable, value, valueFont);
                }
            }
            for (String value : table.totalsRow()) {
                addCell(pdfTable, value, headerFont);
            }

            document.add(pdfTable);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate report", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null || text.isEmpty() ? "-" : text, font));
        cell.setPaddingBottom(6);
        table.addCell(cell);
    }
}
