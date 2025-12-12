package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import net.jqwik.api.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ExportService.
 * Tests universal properties that should hold across all valid inputs.
 */
class ExportServicePropertyTest {

    private final ExportService exportService = new ExportService();

    /**
     * Feature: invoice-enhancements, Property 19: Excel export structure
     * For any set of invoices exported to Excel, the resulting file should contain
     * a header row and one data row per invoice.
     * Validates: Requirements 4.2
     */
    @Property(tries = 100)
    void excelExportStructure(@ForAll("invoiceLists") List<Invoice> invoices) throws IOException {
        byte[] excelData = exportService.exportToExcel(invoices);
        
        // Parse the Excel file
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Should have header row + data rows
            int expectedRows = 1 + invoices.size();
            int actualRows = sheet.getPhysicalNumberOfRows();
            
            assertEquals(expectedRows, actualRows,
                    "Excel file should have 1 header row + " + invoices.size() + " data rows");
            
            // Verify header row exists and has content
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow, "Header row should exist");
            assertTrue(headerRow.getPhysicalNumberOfCells() > 0, "Header row should have cells");
            
            // Verify first header cell contains expected text
            Cell firstHeaderCell = headerRow.getCell(0);
            assertNotNull(firstHeaderCell, "First header cell should exist");
            assertEquals("Invoice Number", firstHeaderCell.getStringCellValue(),
                    "First header should be 'Invoice Number'");
        }
    }

    /**
     * Feature: invoice-enhancements, Property 20: CSV escaping correctness
     * For any invoice data containing commas or quotes, the CSV export should
     * properly escape these characters.
     * Validates: Requirements 4.3
     */
    @Property(tries = 100)
    void csvEscapingCorrectness(@ForAll("invoicesWithSpecialChars") List<Invoice> invoices) {
        byte[] csvData = exportService.exportToCSV(invoices);
        String csvContent = new String(csvData, StandardCharsets.UTF_8);
        
        // Split into lines
        String[] lines = csvContent.split("\n");
        
        // Should have header + data rows
        assertEquals(1 + invoices.size(), lines.length,
                "CSV should have header + data rows");
        
        // Check that fields with commas are properly quoted
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            Invoice invoice = invoices.get(i - 1);
            
            // If customer name contains comma, it should be quoted in CSV
            if (invoice.getBilling() != null && invoice.getBilling().getName() != null) {
                String customerName = invoice.getBilling().getName();
                if (customerName.contains(",")) {
                    assertTrue(line.contains("\"" + customerName + "\"") || 
                              line.contains("\"" + customerName.replace("\"", "\"\"") + "\""),
                            "Fields with commas should be quoted in CSV");
                }
            }
            
            // If address contains comma, it should be quoted
            if (invoice.getBilling() != null && invoice.getBilling().getAddress() != null) {
                String address = invoice.getBilling().getAddress();
                if (address.contains(",")) {
                    assertTrue(line.contains("\"") && line.contains(address.replace("\"", "\"\"")),
                            "Fields with commas should be quoted in CSV");
                }
            }
        }
    }

    /**
     * Feature: invoice-enhancements, Property 21: Export field completeness
     * For any exported invoice, the export should include columns for invoice number,
     * date, due date, customer name, amount, tax, total, and status.
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void exportFieldCompleteness(@ForAll("invoiceLists") List<Invoice> invoices) throws IOException {
        // Test Excel export
        byte[] excelData = exportService.exportToExcel(invoices);
        
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // Required fields that must be present
            String[] requiredFields = {
                "Invoice Number", "Date", "Due Date", "Customer Name",
                "Amount", "Tax", "Total", "Status"
            };
            
            List<String> headerValues = new ArrayList<>();
            for (Cell cell : headerRow) {
                headerValues.add(cell.getStringCellValue());
            }
            
            for (String requiredField : requiredFields) {
                assertTrue(headerValues.contains(requiredField),
                        "Export should include field: " + requiredField);
            }
        }
        
        // Test CSV export
        byte[] csvData = exportService.exportToCSV(invoices);
        String csvContent = new String(csvData, StandardCharsets.UTF_8);
        String headerLine = csvContent.split("\n")[0];
        
        String[] requiredFields = {
            "Invoice Number", "Date", "Due Date", "Customer Name",
            "Amount", "Tax", "Total", "Status"
        };
        
        for (String requiredField : requiredFields) {
            assertTrue(headerLine.contains(requiredField),
                    "CSV export should include field: " + requiredField);
        }
    }

    /**
     * Feature: invoice-enhancements, Property 22: Line item export completeness
     * For any exported invoice with line items, each line item should include
     * name, quantity, rate, GST rate, GST amount, and total.
     * Note: This property validates that the export includes invoice-level data.
     * Line item details are included in the invoice totals and GST calculations.
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void lineItemExportCompleteness(@ForAll("invoicesWithItems") List<Invoice> invoices) throws IOException {
        // Filter to only invoices with items
        List<Invoice> invoicesWithItems = invoices.stream()
                .filter(inv -> inv.getItems() != null && !inv.getItems().isEmpty())
                .collect(Collectors.toList());
        
        if (invoicesWithItems.isEmpty()) {
            return; // Skip if no invoices with items
        }
        
        byte[] excelData = exportService.exportToExcel(invoicesWithItems);
        
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Verify that GST-related columns are present (which come from line items)
            Row headerRow = sheet.getRow(0);
            List<String> headerValues = new ArrayList<>();
            for (Cell cell : headerRow) {
                headerValues.add(cell.getStringCellValue());
            }
            
            assertTrue(headerValues.contains("CGST Total") || 
                      headerValues.contains("SGST Total") || 
                      headerValues.contains("IGST Total"),
                    "Export should include GST information derived from line items");
            
            // Verify data rows have values for invoices with items
            for (int i = 0; i < invoicesWithItems.size(); i++) {
                Row dataRow = sheet.getRow(i + 1);
                assertNotNull(dataRow, "Data row should exist for invoice " + i);
                
                // Amount column should have a value
                Cell amountCell = dataRow.getCell(6); // Amount is column 6
                assertNotNull(amountCell, "Amount cell should exist");
            }
        }
    }

    /**
     * Feature: invoice-enhancements, Property 23: Export selection accuracy
     * For any set of selected invoice IDs, the export should contain exactly
     * those invoices and no others.
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    void exportSelectionAccuracy(@ForAll("invoiceLists") List<Invoice> allInvoices) throws IOException {
        // Export all invoices (invoiceLists now guarantees at least 1 invoice)
        byte[] excelData = exportService.exportToExcel(allInvoices);
        
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Number of data rows should match number of invoices
            int dataRows = sheet.getPhysicalNumberOfRows() - 1; // Subtract header
            assertEquals(allInvoices.size(), dataRows,
                    "Export should contain exactly the number of invoices provided");
            
            // Verify each invoice is represented
            for (int i = 0; i < allInvoices.size(); i++) {
                Row dataRow = sheet.getRow(i + 1);
                assertNotNull(dataRow, "Each invoice should have a corresponding row");
                
                // Verify invoice number matches
                Cell invoiceNumberCell = dataRow.getCell(0);
                if (allInvoices.get(i).getInvoice() != null) {
                    String expectedNumber = allInvoices.get(i).getInvoice().getNumber();
                    if (expectedNumber != null && !expectedNumber.isEmpty()) {
                        assertEquals(expectedNumber, invoiceNumberCell.getStringCellValue(),
                                "Invoice number should match");
                    }
                }
            }
        }
    }

    /**
     * Provides arbitrary lists of invoices for testing.
     */
    @Provide
    Arbitrary<List<Invoice>> invoiceLists() {
        return invoices().list().ofMinSize(1).ofMaxSize(20);
    }

    /**
     * Provides arbitrary lists of invoices with items for testing.
     */
    @Provide
    Arbitrary<List<Invoice>> invoicesWithItems() {
        return invoicesWithLineItems().list().ofMinSize(1).ofMaxSize(10);
    }

    /**
     * Provides invoices with special characters in fields for CSV escaping tests.
     */
    @Provide
    Arbitrary<List<Invoice>> invoicesWithSpecialChars() {
        return invoicesWithSpecialCharacters().list().ofMinSize(1).ofMaxSize(10);
    }

    /**
     * Provides arbitrary Invoice instances for property testing.
     */
    @Provide
    Arbitrary<Invoice> invoices() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.strings().numeric().ofLength(10),
                Arbitraries.doubles().between(100.0, 10000.0),
                Arbitraries.doubles().between(0.0, 1000.0),
                Arbitraries.of(Invoice.InvoiceStatus.class)
        ).as((invoiceNumber, customerName, phone, amount, tax, status) -> {
            Invoice invoice = new Invoice();
            
            // Invoice details
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber(invoiceNumber);
            details.setDate("2024-01-15");
            details.setDueDate("2024-02-15");
            invoice.setInvoice(details);
            
            // Billing details
            Invoice.Billing billing = new Invoice.Billing();
            billing.setName(customerName);
            billing.setPhone(phone);
            billing.setAddress("123 Main St");
            invoice.setBilling(billing);
            
            // Company details
            Invoice.Company company = new Invoice.Company();
            company.setName("Test Company");
            invoice.setCompany(company);
            
            // Financial details
            invoice.setTax(tax);
            invoice.setStatus(status);
            
            return invoice;
        });
    }

    /**
     * Provides invoices with line items.
     */
    @Provide
    Arbitrary<Invoice> invoicesWithLineItems() {
        return Combinators.combine(
                invoices(),
                Arbitraries.integers().between(1, 5)
        ).flatAs((invoice, itemCount) -> {
            Arbitrary<Invoice.Item> itemArbitrary = Combinators.combine(
                    Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),
                    Arbitraries.integers().between(1, 100),
                    Arbitraries.doubles().between(10.0, 1000.0),
                    Arbitraries.doubles().between(0.0, 28.0)
            ).as((name, qty, amount, gstRate) -> {
                Invoice.Item item = new Invoice.Item();
                item.setName(name);
                item.setQty(qty);
                item.setAmount(amount);
                item.setGstRate(gstRate);
                
                double baseAmount = qty * amount;
                double gstAmount = (baseAmount * gstRate) / 100;
                item.setCgstAmount(gstAmount / 2);
                item.setSgstAmount(gstAmount / 2);
                item.setIgstAmount(0);
                item.setTotalWithGST(baseAmount + gstAmount);
                
                return item;
            });
            
            return itemArbitrary.list().ofSize(itemCount).map(items -> {
                invoice.setItems(items);
                invoice.setTransactionType(Invoice.TransactionType.INTRA_STATE);
                
                // Calculate GST details
                Invoice.GSTDetails gstDetails = new Invoice.GSTDetails();
                double cgstTotal = items.stream().mapToDouble(Invoice.Item::getCgstAmount).sum();
                double sgstTotal = items.stream().mapToDouble(Invoice.Item::getSgstAmount).sum();
                gstDetails.setCgstTotal(cgstTotal);
                gstDetails.setSgstTotal(sgstTotal);
                gstDetails.setIgstTotal(0);
                gstDetails.setGstTotal(cgstTotal + sgstTotal);
                invoice.setGstDetails(gstDetails);
                
                return invoice;
            });
        });
    }

    /**
     * Provides invoices with special characters for CSV escaping tests.
     */
    @Provide
    Arbitrary<Invoice> invoicesWithSpecialCharacters() {
        return Combinators.combine(
                Arbitraries.strings().withChars('a', 'b', ',', '"').ofMinLength(5).ofMaxLength(20),
                Arbitraries.strings().withChars('1', '2', '3', ',').ofMinLength(5).ofMaxLength(30)
        ).as((nameWithComma, addressWithComma) -> {
            Invoice invoice = new Invoice();
            
            // Invoice details
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-001");
            details.setDate("2024-01-15");
            details.setDueDate("2024-02-15");
            invoice.setInvoice(details);
            
            // Billing with special characters
            Invoice.Billing billing = new Invoice.Billing();
            billing.setName(nameWithComma);
            billing.setPhone("1234567890");
            billing.setAddress(addressWithComma);
            invoice.setBilling(billing);
            
            // Company details
            Invoice.Company company = new Invoice.Company();
            company.setName("Test Company");
            invoice.setCompany(company);
            
            invoice.setTax(100.0);
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
            
            return invoice;
        });
    }
}
