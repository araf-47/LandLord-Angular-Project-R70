package com.landlord.backend.billing;

import com.landlord.backend.property.Property;
import com.landlord.backend.property.PropertyRepository;
import com.landlord.backend.tenant.Tenant;
import com.landlord.backend.tenant.TenantRepository;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Phase 10.5: one-page payment receipt, generated on demand rather than stored -
 * everything it needs (payment/invoice/tenant/unit/property) already exists as a
 * real row, so there's no separate "receipt" record to keep in sync.
 */
@Service
public class ReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final TenantRepository tenants;
    private final UnitRepository units;
    private final PropertyRepository properties;

    public ReceiptService(PaymentRepository payments, InvoiceRepository invoices, TenantRepository tenants,
            UnitRepository units, PropertyRepository properties) {
        this.payments = payments;
        this.invoices = invoices;
        this.tenants = tenants;
        this.units = units;
        this.properties = properties;
    }

    public byte[] generateReceipt(Long paymentId) {
        Payment payment = payments.findById(paymentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        Invoice invoice = invoices.findById(payment.getInvoiceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        Tenant tenant = tenants.findById(payment.getTenantId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        Unit unit = units.findById(invoice.getUnitId()).orElse(null);
        Property property = unit == null ? null : properties.findById(unit.getPropertyId()).orElse(null);

        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("Payment Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Receipt #" + payment.getId(), valueFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {1.2f, 2f});

            addRow(table, "Tenant", tenant.getName(), labelFont, valueFont);
            if (property != null) {
                addRow(table, "Property", property.getName(), labelFont, valueFont);
            }
            if (unit != null) {
                addRow(table, "Unit", unit.getUnitNumber(), labelFont, valueFont);
            }
            addRow(table, "Billing period", invoice.getPeriod(), labelFont, valueFont);
            addRow(table, "Payment date",
                DATE_FORMAT.format(payment.getDate().atZone(ZoneId.systemDefault())), labelFont, valueFont);
            addRow(table, "Payment method", capitalize(payment.getMethod()), labelFont, valueFont);
            addRow(table, "Amount paid", formatTaka(payment.getAmount()), labelFont, valueFont);
            addRow(table, "Invoice total", formatTaka(invoice.getAmount()), labelFont, valueFont);
            addRow(table, "Remaining balance", formatTaka(invoice.getBalance()), labelFont, valueFont);
            addRow(table, "Invoice status", capitalize(invoice.getStatus()), labelFont, valueFont);

            document.add(table);

            Paragraph footer = new Paragraph(
                "\nThis is a system-generated receipt for the payment recorded above.", valueFont);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate receipt", e);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(8);
        PdfPCell valueCell = new PdfPCell(new Paragraph(value == null ? "-" : value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(8);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * "Tk" not "৳" - the standard PDF base fonts (Helvetica) only cover WinAnsi
     * (Latin-1); the Bengali Taka glyph silently drops out with no error,
     * printing as a blank instead of throwing. Embedding a Unicode font just for
     * one symbol isn't worth it here.
     */
    private String formatTaka(Double amount) {
        return amount == null ? "-" : String.format("Tk %,.2f", amount);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
