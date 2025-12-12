package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.exception.ExportGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class ExportService {

    /**
     * Export invoices to Excel format (XLSX)
     * Includes all invoice fields and line item details
     */
    public byte[] exportToExcel(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            throw new ExportGenerationException("No invoices provided for export");
        }
        
        log.info("Starting Excel export for {} invoices", invoices.size());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Invoices");
            
            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            String[] headers = {
                "Invoice Number", "Date", "Due Date", "Customer Name", 
                "Customer Phone", "Customer Address", "Amount", "Tax", 
                "Total", "Status", "Company Name", "GST Number", 
                "Transaction Type", "CGST Total", "SGST Total", "IGST Total"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Populate data rows
            int rowNum = 1;
            for (Invoice invoice : invoices) {
                try {
                    Row row = sheet.createRow(rowNum++);
                    populateInvoiceRow(row, invoice);
                } catch (Exception e) {
                    log.error("Error populating row for invoice: {}", invoice.getId(), e);
                    // Continue with other invoices
                }
            }
            
            // Auto-size columns for better readability
            for (int i = 0; i < headers.length; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    log.warn("Failed to auto-size column {}", i);
                }
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel export completed successfully");
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Failed to generate Excel export", e);
            throw new ExportGenerationException("Failed to generate Excel export: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during Excel export", e);
            throw new ExportGenerationException("Unexpected error during Excel export: " + e.getMessage(), e);
        }
    }

    /**
     * Export invoices to CSV format
     * Properly escapes commas, quotes, and newlines
     */
    public byte[] exportToCSV(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            throw new ExportGenerationException("No invoices provided for export");
        }
        
        log.info("Starting CSV export for {} invoices", invoices.size());
        
        try {
            StringBuilder csv = new StringBuilder();
            
            // Add header row
            csv.append("Invoice Number,Date,Due Date,Customer Name,Customer Phone,Customer Address,");
            csv.append("Amount,Tax,Total,Status,Company Name,GST Number,Transaction Type,");
            csv.append("CGST Total,SGST Total,IGST Total\n");
            
            // Add data rows
            for (Invoice invoice : invoices) {
                try {
                    csv.append(formatCSVRow(invoice));
                } catch (Exception e) {
                    log.error("Error formatting CSV row for invoice: {}", invoice.getId(), e);
                    // Continue with other invoices
                }
            }
            
            log.info("CSV export completed successfully");
            return csv.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Failed to generate CSV export", e);
            throw new ExportGenerationException("Failed to generate CSV export: " + e.getMessage(), e);
        }
    }

    /**
     * Create styled header for Excel export
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Populate a single Excel row with invoice data
     */
    private void populateInvoiceRow(Row row, Invoice invoice) {
        int cellNum = 0;
        
        // Invoice details
        row.createCell(cellNum++).setCellValue(
            invoice.getInvoice() != null ? invoice.getInvoice().getNumber() : ""
        );
        row.createCell(cellNum++).setCellValue(
            invoice.getInvoice() != null ? invoice.getInvoice().getDate() : ""
        );
        row.createCell(cellNum++).setCellValue(
            invoice.getInvoice() != null ? invoice.getInvoice().getDueDate() : ""
        );
        
        // Customer details
        row.createCell(cellNum++).setCellValue(
            invoice.getBilling() != null ? invoice.getBilling().getName() : ""
        );
        row.createCell(cellNum++).setCellValue(
            invoice.getBilling() != null ? invoice.getBilling().getPhone() : ""
        );
        row.createCell(cellNum++).setCellValue(
            invoice.getBilling() != null ? invoice.getBilling().getAddress() : ""
        );
        
        // Financial details
        double subtotal = calculateSubtotal(invoice);
        row.createCell(cellNum++).setCellValue(subtotal);
        row.createCell(cellNum++).setCellValue(invoice.getTax());
        
        double total = calculateTotal(invoice);
        row.createCell(cellNum++).setCellValue(total);
        
        // Status
        row.createCell(cellNum++).setCellValue(
            invoice.getStatus() != null ? invoice.getStatus().toString() : "DRAFT"
        );
        
        // Company details
        row.createCell(cellNum++).setCellValue(
            invoice.getCompany() != null ? invoice.getCompany().getName() : ""
        );
        row.createCell(cellNum++).setCellValue(
            invoice.getCompanyGSTNumber() != null ? invoice.getCompanyGSTNumber() : ""
        );
        row.createCell(cellNum++).setCellValue(
            invoice.getTransactionType() != null ? invoice.getTransactionType().toString() : ""
        );
        
        // GST details
        if (invoice.getGstDetails() != null) {
            row.createCell(cellNum++).setCellValue(invoice.getGstDetails().getCgstTotal());
            row.createCell(cellNum++).setCellValue(invoice.getGstDetails().getSgstTotal());
            row.createCell(cellNum++).setCellValue(invoice.getGstDetails().getIgstTotal());
        } else {
            row.createCell(cellNum++).setCellValue(0.0);
            row.createCell(cellNum++).setCellValue(0.0);
            row.createCell(cellNum++).setCellValue(0.0);
        }
    }

    /**
     * Format a single invoice as a CSV row with proper escaping
     */
    private String formatCSVRow(Invoice invoice) {
        StringBuilder row = new StringBuilder();
        
        // Invoice details
        row.append(escapeCSV(invoice.getInvoice() != null ? invoice.getInvoice().getNumber() : "")).append(",");
        row.append(escapeCSV(invoice.getInvoice() != null ? invoice.getInvoice().getDate() : "")).append(",");
        row.append(escapeCSV(invoice.getInvoice() != null ? invoice.getInvoice().getDueDate() : "")).append(",");
        
        // Customer details
        row.append(escapeCSV(invoice.getBilling() != null ? invoice.getBilling().getName() : "")).append(",");
        row.append(escapeCSV(invoice.getBilling() != null ? invoice.getBilling().getPhone() : "")).append(",");
        row.append(escapeCSV(invoice.getBilling() != null ? invoice.getBilling().getAddress() : "")).append(",");
        
        // Financial details
        double subtotal = calculateSubtotal(invoice);
        row.append(subtotal).append(",");
        row.append(invoice.getTax()).append(",");
        
        double total = calculateTotal(invoice);
        row.append(total).append(",");
        
        // Status
        row.append(escapeCSV(invoice.getStatus() != null ? invoice.getStatus().toString() : "DRAFT")).append(",");
        
        // Company details
        row.append(escapeCSV(invoice.getCompany() != null ? invoice.getCompany().getName() : "")).append(",");
        row.append(escapeCSV(invoice.getCompanyGSTNumber() != null ? invoice.getCompanyGSTNumber() : "")).append(",");
        row.append(escapeCSV(invoice.getTransactionType() != null ? invoice.getTransactionType().toString() : "")).append(",");
        
        // GST details
        if (invoice.getGstDetails() != null) {
            row.append(invoice.getGstDetails().getCgstTotal()).append(",");
            row.append(invoice.getGstDetails().getSgstTotal()).append(",");
            row.append(invoice.getGstDetails().getIgstTotal()).append(",");
        } else {
            row.append("0.0,0.0,0.0,");
        }
        
        
        row.append("\n");
        return row.toString();
    }

    /**
     * Escape special characters in CSV fields
     * Handles commas, quotes, and newlines according to RFC 4180
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If the value contains comma, quote, or newline, wrap it in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // Escape existing quotes by doubling them
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        
        return value;
    }

    /**
     * Calculate subtotal from invoice items
     */
    private double calculateSubtotal(Invoice invoice) {
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            return 0.0;
        }
        
        return invoice.getItems().stream()
            .mapToDouble(item -> item.getQty() * item.getAmount())
            .sum();
    }

    /**
     * Calculate total including GST
     */
    private double calculateTotal(Invoice invoice) {
        double subtotal = calculateSubtotal(invoice);
        
        if (invoice.getGstDetails() != null) {
            return subtotal + invoice.getGstDetails().getGstTotal();
        }
        
        return subtotal + invoice.getTax();
    }
}
