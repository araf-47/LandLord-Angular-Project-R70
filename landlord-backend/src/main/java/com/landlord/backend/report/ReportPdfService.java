package com.landlord.backend.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Extends the receipt PDF pattern from billing/ReceiptService.java - in-memory OpenPDF document, nothing persisted. */
@Service
public class ReportPdfService {

    private static final String LOGO_RESOURCE = "/report-assets/landlordcore-logo.png";
    private static final java.awt.Color BRAND_BLUE = new java.awt.Color(0x2F, 0x7F, 0xE0);
    private static final java.awt.Color HEADER_ROW_BG = new java.awt.Color(0x0F, 0x2F, 0x5F);
    private static final java.awt.Color ALT_ROW_BG = new java.awt.Color(0xF3, 0xF6, 0xFB);
    private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");

    public byte[] render(ReportTable table) {
        try {
            Document document = new Document(com.lowagie.text.PageSize.A4, 40, 40, 70, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new ReportPageEvent(table.title()));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, HEADER_ROW_BG);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, java.awt.Color.DARK_GRAY);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, java.awt.Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.DARK_GRAY);
            Font totalsFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_ROW_BG);

            Paragraph title = new Paragraph(table.title(), titleFont);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph(table.subtitle(), subtitleFont);
            subtitle.setSpacingAfter(2);
            document.add(subtitle);

            Paragraph generatedAt = new Paragraph(
                "Generated on " + LocalDateTime.now().format(GENERATED_AT_FORMAT), metaFont);
            generatedAt.setSpacingAfter(16);
            document.add(generatedAt);

            PdfPTable pdfTable = new PdfPTable(table.headers().size());
            pdfTable.setWidthPercentage(100);
            pdfTable.setHeaderRows(1);

            boolean[] numericColumn = detectNumericColumns(table);

            for (String header : table.headers()) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(HEADER_ROW_BG);
                cell.setPaddingTop(7);
                cell.setPaddingBottom(7);
                cell.setPaddingLeft(6);
                cell.setPaddingRight(6);
                cell.setBorderColor(HEADER_ROW_BG);
                pdfTable.addCell(cell);
            }

            int rowIndex = 0;
            for (List<String> row : table.rows()) {
                java.awt.Color rowBg = rowIndex % 2 == 1 ? ALT_ROW_BG : java.awt.Color.WHITE;
                for (int col = 0; col < row.size(); col++) {
                    addCell(pdfTable, row.get(col), valueFont, rowBg, numericColumn[col]);
                }
                rowIndex++;
            }

            for (int col = 0; col < table.totalsRow().size(); col++) {
                PdfPCell cell = new PdfPCell(new Phrase(displayValue(table.totalsRow().get(col)), totalsFont));
                cell.setPaddingTop(7);
                cell.setPaddingBottom(7);
                cell.setPaddingLeft(6);
                cell.setPaddingRight(6);
                cell.setBorder(Rectangle.TOP);
                cell.setBorderWidth(1.2f);
                cell.setBorderColor(HEADER_ROW_BG);
                cell.setHorizontalAlignment(numericColumn[col] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                pdfTable.addCell(cell);
            }

            document.add(pdfTable);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate report", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, java.awt.Color background, boolean numeric) {
        PdfPCell cell = new PdfPCell(new Phrase(displayValue(text), font));
        cell.setBackgroundColor(background);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        cell.setBorderColor(new java.awt.Color(0xE3, 0xE8, 0xEF));
        cell.setHorizontalAlignment(numeric ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private String displayValue(String text) {
        return text == null || text.isEmpty() ? "-" : text;
    }

    /** A column is right-aligned if every non-empty value in it looks numeric/currency. */
    private boolean[] detectNumericColumns(ReportTable table) {
        int columnCount = table.headers().size();
        boolean[] numeric = new boolean[columnCount];
        java.util.Arrays.fill(numeric, true);
        for (List<String> row : table.rows()) {
            for (int col = 0; col < row.size() && col < columnCount; col++) {
                String value = row.get(col);
                if (value != null && !value.isEmpty() && !looksNumeric(value)) {
                    numeric[col] = false;
                }
            }
        }
        return numeric;
    }

    private boolean looksNumeric(String value) {
        String stripped = value.trim().replaceAll("[,৳$%()\\s]", "");
        if (stripped.isEmpty()) {
            return false;
        }
        return stripped.matches("-?\\d+(\\.\\d+)?");
    }

    /** Repeating page header (logo + report title) and footer (page X of Y + timestamp). */
    private static class ReportPageEvent extends PdfPageEventHelper {
        private final String reportTitle;
        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, java.awt.Color.GRAY);
        private final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, HEADER_ROW_BG);
        private Image logo;
        private PdfTemplate totalPagesTemplate;

        ReportPageEvent(String reportTitle) {
            this.reportTitle = reportTitle;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                var resource = getClass().getResource(LOGO_RESOURCE);
                if (resource != null) {
                    logo = Image.getInstance(resource);
                    logo.scaleToFit(90, 30);
                }
            } catch (Exception ignored) {
                logo = null;
            }
            totalPagesTemplate = writer.getDirectContent().createTemplate(50, 20);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable header = new PdfPTable(2);
            try {
                header.setWidths(new float[] {1f, 3f});
                header.setTotalWidth(document.right() - document.left());
                PdfPCell logoCell = new PdfPCell();
                logoCell.setBorder(Rectangle.NO_BORDER);
                if (logo != null) {
                    logoCell.addElement(logo);
                } else {
                    logoCell.addElement(new Phrase("LandLord", headerFont));
                }
                header.addCell(logoCell);

                PdfPCell titleCell = new PdfPCell(new Phrase(reportTitle, headerFont));
                titleCell.setBorder(Rectangle.NO_BORDER);
                titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                header.addCell(titleCell);

                header.writeSelectedRows(0, -1, document.left(), document.top() + 45, writer.getDirectContent());

                com.lowagie.text.pdf.PdfContentByte canvas = writer.getDirectContent();
                canvas.setColorStroke(BRAND_BLUE);
                canvas.setLineWidth(1f);
                canvas.moveTo(document.left(), document.top() + 8);
                canvas.lineTo(document.right(), document.top() + 8);
                canvas.stroke();

                Phrase generatedText = new Phrase(
                    "Page " + writer.getPageNumber() + " of ", footerFont);
                float footerY = document.bottom() - 25;
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                    canvas, Element.ALIGN_LEFT, generatedText, document.left(), footerY, 0);

                float textWidth = footerFont.getBaseFont().getWidthPoint(
                    "Page " + writer.getPageNumber() + " of ", footerFont.getSize());
                canvas.addTemplate(totalPagesTemplate, document.left() + textWidth, footerY);

                Phrase timestamp = new Phrase(
                    "Generated " + LocalDateTime.now().format(GENERATED_AT_FORMAT), footerFont);
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                    canvas, Element.ALIGN_RIGHT, timestamp, document.right(), footerY, 0);

                canvas.setColorStroke(new java.awt.Color(0xE3, 0xE8, 0xEF));
                canvas.moveTo(document.left(), document.bottom() - 15);
                canvas.lineTo(document.right(), document.bottom() - 15);
                canvas.stroke();
            } catch (Exception ignored) {
                // header/footer are cosmetic; never fail report generation over them
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(footerFont.getBaseFont(), footerFont.getSize());
            totalPagesTemplate.setColorFill(java.awt.Color.GRAY);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPagesTemplate.endText();
        }
    }
}
