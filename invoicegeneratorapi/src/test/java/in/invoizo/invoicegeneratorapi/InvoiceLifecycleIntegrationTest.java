package in.invoizo.invoicegeneratorapi;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.entity.Invoice.TransactionType;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import in.invoizo.invoicegeneratorapi.service.*;
import in.invoizo.invoicegeneratorapi.validator.StatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for complete invoice lifecycle
 * Tests the full flow from creation to payment and export
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test_invoice_db"
})
class InvoiceLifecycleIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private GSTCalculatorService gstCalculatorService;

    @Autowired
    private StatusTransitionValidator statusTransitionValidator;

    @Autowired
    private ExportService exportService;

    private String testClerkId = "test_user_123";

    @BeforeEach
    void setUp() {
        // Clean up test data
        invoiceRepository.deleteAll();
    }

    /**
     * Test 20.1: Complete invoice lifecycle
     * - Create invoice with GST
     * - Send invoice with payment link
     * - Simulate payment webhook
     * - Verify status updates
     * - Export invoice data
     */
    @Test
    void testCompleteInvoiceLifecycle() {
        // Step 1: Create invoice with GST
        Invoice invoice = createTestInvoiceWithGST();
        
        // Verify initial status is DRAFT
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus(), 
            "New invoice should have DRAFT status");
        assertNotNull(invoice.getId(), "Invoice should have an ID");
        
        // Verify GST calculations
        assertNotNull(invoice.getGstDetails(), "Invoice should have GST details");
        assertTrue(invoice.getGstDetails().getGstTotal() > 0, 
            "GST total should be calculated");
        
        // For INTRA_STATE, CGST and SGST should be equal
        if (invoice.getTransactionType() == TransactionType.INTRA_STATE) {
            assertEquals(invoice.getGstDetails().getCgstTotal(), 
                invoice.getGstDetails().getSgstTotal(),
                "CGST and SGST should be equal for intra-state transactions");
            assertEquals(0.0, invoice.getGstDetails().getIgstTotal(),
                "IGST should be zero for intra-state transactions");
        }
        
        // Step 2: Send invoice (update status to SENT)
        Invoice sentInvoice = invoiceService.updateStatus(
            testClerkId,
            invoice.getId(), 
            InvoiceStatus.SENT
        );
        
        assertEquals(InvoiceStatus.SENT, sentInvoice.getStatus(),
            "Invoice status should be SENT");
        assertNotNull(sentInvoice.getSentAt(), 
            "Sent timestamp should be recorded");
        
        // Step 3: Simulate payment (update status to PAID)
        Invoice paidInvoice = invoiceService.updateStatus(
            testClerkId,
            sentInvoice.getId(),
            InvoiceStatus.PAID
        );
        
        assertEquals(InvoiceStatus.PAID, paidInvoice.getStatus(),
            "Invoice status should be PAID");
        assertNotNull(paidInvoice.getPaidAt(),
            "Paid timestamp should be recorded");
        
        // Step 4: Verify status updates are persisted
        Invoice retrievedInvoice = invoiceRepository.findById(invoice.getId())
            .orElseThrow(() -> new AssertionError("Invoice should exist"));
        
        assertEquals(InvoiceStatus.PAID, retrievedInvoice.getStatus(),
            "Status should be persisted in database");
        assertNotNull(retrievedInvoice.getSentAt(),
            "Sent timestamp should be persisted");
        assertNotNull(retrievedInvoice.getPaidAt(),
            "Paid timestamp should be persisted");
        
        // Step 5: Export invoice data
        List<Invoice> invoicesToExport = List.of(retrievedInvoice);
        
        // Test Excel export
        byte[] excelData = exportService.exportToExcel(invoicesToExport);
        assertNotNull(excelData, "Excel export should produce data");
        assertTrue(excelData.length > 0, "Excel data should not be empty");
        
        // Test CSV export
        byte[] csvData = exportService.exportToCSV(invoicesToExport);
        assertNotNull(csvData, "CSV export should produce data");
        assertTrue(csvData.length > 0, "CSV data should not be empty");
        
        // Verify CSV contains invoice data
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains(invoice.getInvoice().getNumber()),
            "CSV should contain invoice number");
        assertTrue(csvContent.contains("PAID"),
            "CSV should contain invoice status");
    }

    /**
     * Test 20.2: Edge cases
     */
    @Test
    void testEdgeCases() {
        // Test with zero GST rate
        Invoice zeroGSTInvoice = createInvoiceWithGSTRate(0.0);
        assertEquals(0.0, zeroGSTInvoice.getGstDetails().getGstTotal(),
            "Zero GST rate should result in zero GST total");
        
        // Test with maximum GST rate (28%)
        Invoice maxGSTInvoice = createInvoiceWithGSTRate(28.0);
        assertTrue(maxGSTInvoice.getGstDetails().getGstTotal() > 0,
            "28% GST rate should calculate GST");
        
        // Verify GST calculation for 28% rate
        double expectedGST = 1000.0 * 0.28; // Base amount * rate
        assertEquals(expectedGST, maxGSTInvoice.getGstDetails().getGstTotal(), 0.01,
            "GST should be calculated correctly for 28% rate");
    }

    @Test
    void testStatusTransitionsFromAllStates() {
        // Test valid transitions from DRAFT
        assertTrue(statusTransitionValidator.isValidTransition(
            InvoiceStatus.DRAFT, InvoiceStatus.SENT),
            "DRAFT -> SENT should be valid");
        assertTrue(statusTransitionValidator.isValidTransition(
            InvoiceStatus.DRAFT, InvoiceStatus.CANCELLED),
            "DRAFT -> CANCELLED should be valid");
        
        // Test valid transitions from SENT
        assertTrue(statusTransitionValidator.isValidTransition(
            InvoiceStatus.SENT, InvoiceStatus.VIEWED),
            "SENT -> VIEWED should be valid");
        assertTrue(statusTransitionValidator.isValidTransition(
            InvoiceStatus.SENT, InvoiceStatus.PAID),
            "SENT -> PAID should be valid");
        assertTrue(statusTransitionValidator.isValidTransition(
            InvoiceStatus.SENT, InvoiceStatus.OVERDUE),
            "SENT -> OVERDUE should be valid");
        
        // Test invalid transitions
        assertFalse(statusTransitionValidator.isValidTransition(
            InvoiceStatus.PAID, InvoiceStatus.DRAFT),
            "PAID -> DRAFT should be invalid");
        assertFalse(statusTransitionValidator.isValidTransition(
            InvoiceStatus.CANCELLED, InvoiceStatus.SENT),
            "CANCELLED -> SENT should be invalid");
    }

    @Test
    void testExportWithEmptyDataset() {
        // Test export with empty list
        List<Invoice> emptyList = new ArrayList<>();
        
        assertThrows(Exception.class, () -> {
            exportService.exportToExcel(emptyList);
        }, "Export with empty dataset should throw exception");
        
        assertThrows(Exception.class, () -> {
            exportService.exportToCSV(null);
        }, "Export with null dataset should throw exception");
    }

    /**
     * Helper method to create a test invoice with GST
     */
    private Invoice createTestInvoiceWithGST() {
        Invoice invoice = new Invoice();
        invoice.setClerkId(testClerkId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTransactionType(TransactionType.INTRA_STATE);
        invoice.setCompanyGSTNumber("29ABCDE1234F1Z5");
        
        // Create invoice details
        Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
        details.setNumber("INV-001");
        details.setDate(LocalDate.now().toString());
        details.setDueDate(LocalDate.now().plusDays(30).toString());
        invoice.setInvoice(details);
        
        // Create company details
        Invoice.Company company = new Invoice.Company();
        company.setName("Test Company");
        company.setPhone("1234567890");
        company.setAddress("Test Address");
        invoice.setCompany(company);
        
        // Create billing details
        Invoice.Billing billing = new Invoice.Billing();
        billing.setName("Test Customer");
        billing.setPhone("0987654321");
        billing.setAddress("Customer Address");
        invoice.setBilling(billing);
        
        // Create line items with GST
        List<Invoice.Item> items = new ArrayList<>();
        Invoice.Item item = new Invoice.Item();
        item.setName("Test Product");
        item.setQty(10);
        item.setAmount(100.0);
        item.setGstRate(18.0);
        items.add(item);
        invoice.setItems(items);
        
        // Calculate GST for items first
        for (Invoice.Item itm : items) {
            double baseAmount = itm.getQty() * itm.getAmount();
            GSTCalculatorService.GSTCalculation gstCalc = gstCalculatorService.calculateGST(
                baseAmount, 
                itm.getGstRate(), 
                invoice.getTransactionType()
            );
            itm.setCgstAmount(gstCalc.getCgst());
            itm.setSgstAmount(gstCalc.getSgst());
            itm.setIgstAmount(gstCalc.getIgst());
            itm.setTotalWithGST(baseAmount + gstCalc.getTotal());
        }
        
        // Calculate invoice-level GST totals
        Invoice.GSTDetails gstDetails = gstCalculatorService.calculateInvoiceGST(invoice);
        invoice.setGstDetails(gstDetails);
        
        // Save invoice
        return invoiceRepository.save(invoice);
    }

    /**
     * Helper method to create invoice with specific GST rate
     */
    private Invoice createInvoiceWithGSTRate(double gstRate) {
        Invoice invoice = new Invoice();
        invoice.setClerkId(testClerkId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTransactionType(TransactionType.INTRA_STATE);
        
        // Create invoice details
        Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
        details.setNumber("INV-" + System.currentTimeMillis());
        details.setDate(LocalDate.now().toString());
        details.setDueDate(LocalDate.now().plusDays(30).toString());
        invoice.setInvoice(details);
        
        // Create company details
        Invoice.Company company = new Invoice.Company();
        company.setName("Test Company");
        invoice.setCompany(company);
        
        // Create billing details
        Invoice.Billing billing = new Invoice.Billing();
        billing.setName("Test Customer");
        invoice.setBilling(billing);
        
        // Create line item with specified GST rate
        List<Invoice.Item> items = new ArrayList<>();
        Invoice.Item item = new Invoice.Item();
        item.setName("Test Product");
        item.setQty(10);
        item.setAmount(100.0);
        item.setGstRate(gstRate);
        items.add(item);
        invoice.setItems(items);
        
        // Calculate GST for items first
        for (Invoice.Item itm : items) {
            double baseAmount = itm.getQty() * itm.getAmount();
            GSTCalculatorService.GSTCalculation gstCalc = gstCalculatorService.calculateGST(
                baseAmount, 
                itm.getGstRate(), 
                invoice.getTransactionType()
            );
            itm.setCgstAmount(gstCalc.getCgst());
            itm.setSgstAmount(gstCalc.getSgst());
            itm.setIgstAmount(gstCalc.getIgst());
            itm.setTotalWithGST(baseAmount + gstCalc.getTotal());
        }
        
        // Calculate invoice-level GST totals
        Invoice.GSTDetails gstDetails = gstCalculatorService.calculateInvoiceGST(invoice);
        invoice.setGstDetails(gstDetails);
        
        // Save invoice
        return invoiceRepository.save(invoice);
    }
}
