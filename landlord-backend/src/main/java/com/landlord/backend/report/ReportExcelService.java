package com.landlord.backend.report;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Mirrors ReportPdfService.render - one generic table renderer shared by all four fixed reports. */
@Service
public class ReportExcelService {

    public byte[] render(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(table.title());

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(0);
            List<String> headers = table.headers();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (List<String> row : table.rows()) {
                Row r = sheet.createRow(rowNum++);
                for (int i = 0; i < row.size(); i++) {
                    r.createCell(i).setCellValue(row.get(i));
                }
            }

            Row totalRow = sheet.createRow(rowNum);
            List<String> totals = table.totalsRow();
            for (int i = 0; i < totals.size(); i++) {
                Cell cell = totalRow.createCell(i);
                cell.setCellValue(totals.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate report", e);
        }
    }
}
