package com.landlord.backend.report;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;
    private final ReportExcelService reportExcelService;
    private final ReportTables reportTables;

    public ReportController(ReportService reportService, ReportPdfService reportPdfService,
            ReportExcelService reportExcelService, ReportTables reportTables) {
        this.reportService = reportService;
        this.reportPdfService = reportPdfService;
        this.reportExcelService = reportExcelService;
        this.reportTables = reportTables;
    }

    // --- Income statement ---

    @GetMapping("/income-statement")
    public IncomeStatementReport incomeStatement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId) {
        return reportService.incomeStatement(startDate, endDate, propertyId);
    }

    @GetMapping("/income-statement.pdf")
    public ResponseEntity<byte[]> incomeStatementPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId) {
        ReportTable table = reportTables.of(reportService.incomeStatement(startDate, endDate, propertyId));
        return pdfResponse(table, "income-statement.pdf");
    }

    @GetMapping("/income-statement.xlsx")
    public ResponseEntity<byte[]> incomeStatementExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId) {
        ReportTable table = reportTables.of(reportService.incomeStatement(startDate, endDate, propertyId));
        return excelResponse(table, "income-statement.xlsx");
    }

    // --- Expense report ---

    @GetMapping("/expense-report")
    public ExpenseReport expenseReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String category) {
        return reportService.expenseReport(startDate, endDate, propertyId, category);
    }

    @GetMapping("/expense-report.pdf")
    public ResponseEntity<byte[]> expenseReportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String category) {
        ReportTable table = reportTables.of(reportService.expenseReport(startDate, endDate, propertyId, category));
        return pdfResponse(table, "expense-report.pdf");
    }

    @GetMapping("/expense-report.xlsx")
    public ResponseEntity<byte[]> expenseReportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String category) {
        ReportTable table = reportTables.of(reportService.expenseReport(startDate, endDate, propertyId, category));
        return excelResponse(table, "expense-report.xlsx");
    }

    // --- Occupancy report ---

    @GetMapping("/occupancy-report")
    public OccupancyReport occupancyReport(@RequestParam(required = false) Long propertyId) {
        return reportService.occupancyReport(propertyId);
    }

    @GetMapping("/occupancy-report.pdf")
    public ResponseEntity<byte[]> occupancyReportPdf(@RequestParam(required = false) Long propertyId) {
        ReportTable table = reportTables.of(reportService.occupancyReport(propertyId));
        return pdfResponse(table, "occupancy-report.pdf");
    }

    @GetMapping("/occupancy-report.xlsx")
    public ResponseEntity<byte[]> occupancyReportExcel(@RequestParam(required = false) Long propertyId) {
        ReportTable table = reportTables.of(reportService.occupancyReport(propertyId));
        return excelResponse(table, "occupancy-report.xlsx");
    }

    // --- Tenant ledger ---

    @GetMapping("/tenant-ledger")
    public TenantLedgerReport tenantLedger(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.tenantLedgerReport(tenantId, startDate, endDate);
    }

    @GetMapping("/tenant-ledger.pdf")
    public ResponseEntity<byte[]> tenantLedgerPdf(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ReportTable table = reportTables.of(reportService.tenantLedgerReport(tenantId, startDate, endDate));
        return pdfResponse(table, "tenant-ledger.pdf");
    }

    @GetMapping("/tenant-ledger.xlsx")
    public ResponseEntity<byte[]> tenantLedgerExcel(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ReportTable table = reportTables.of(reportService.tenantLedgerReport(tenantId, startDate, endDate));
        return excelResponse(table, "tenant-ledger.xlsx");
    }

    private ResponseEntity<byte[]> pdfResponse(ReportTable table, String filename) {
        byte[] pdf = reportPdfService.render(table);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private ResponseEntity<byte[]> excelResponse(ReportTable table, String filename) {
        byte[] xlsx = reportExcelService.render(table);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(xlsx);
    }
}
