package in.invoizo.invoicegeneratorapi;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.entity.Invoice.TransactionType;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import in.invoizo.invoicegeneratorapi.scheduler.InvoiceStatusScheduler;
import in.invoizo.invoicegeneratorapi.service.ExportService;
import in.invoizo.invoicegeneratorapi.service.GSTCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance integration tests
 * Tests system behavior under load and with large datasets
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test_invoice_db"
})
class PerformanceIntegrationTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ExportService exportService;

    @Autowired
    private GSTCalculatorService gstCalculatorService;

    @Autowired
    private InvoiceStatusScheduler invoiceStatusScheduler;

    private String testClerkId = "perf_test_user";

    @BeforeEach
    void setUp() {
        // Clean up test data
        invoiceRepository.deleteAll();
    }

    /**
     * Test 20.3: Performance testing
     * - Test export with large dataset (1000+ invoices)
     * - Test overdue detection with many invoices
     * - Verify database query performance with indexes
     * - Test concurrent webhook processing
     */
    @Test
    void testExportWithLargeDataset() {
        // Create 1000 test invoices
        int invoiceCount = 1000;
        List<Invoice> invoices = new ArrayList<>();
        
        System.out.println("Creating " + invoiceCount + " test invoices...");
        long startCreate = System.currentTimeMillis();
        
        for (int i = 0; i < invoiceCount; i++) {
            Invoice invoice = createTestInvoice(i);
            invoices.add(invoice);
        }
        
        // Batch save for better performance
        invoiceRepository.saveAll(invoices);
        long endCreate = System.currentTimeMillis();
        System.out.println("Created " + invoiceCount + " invoices in " + (endCreate - startCreate) + "ms");
        
        // Test Excel export performance
        System.out.println("Testing Excel export with " + invoiceCount + " invoices...");
        long startExcel = System.currentTimeMillis();
        byte[] excelData = exportService.exportToExcel(invoices);
        long endExcel = System.currentTimeMillis();
        
        assertNotNull(excelData, "Excel export should produce data");
        assertTrue(excelData.length > 0, "Excel data should not be empty");
        long excelTime = endExcel - startExcel;
        System.out.println("Excel export completed in " + excelTime + "ms");
        
        // Excel export should complete within reasonable time (30 seconds for 1000 invoices)
        assertTrue(excelTime < 30000, 
            "Excel export should complete within 30 seconds, took " + excelTime + "ms");
        
        // Test CSV export performance
        System.out.println("Testing CSV export with " + invoiceCount + " invoices...");
        long startCsv = System.currentTimeMillis();
        byte[] csvData = exportService.exportToCSV(invoices);
        long endCsv = System.currentTimeMillis();
        
        assertNotNull(csvData, "CSV export should produce data");
        assertTrue(csvData.length > 0, "CSV data should not be empty");
        long csvTime = endCsv - startCsv;
        System.out.println("CSV export completed in " + csvTime + "ms");
        
        // CSV export should be faster than Excel (10 seconds for 1000 invoices)
        assertTrue(csvTime < 10000, 
            "CSV export should complete within 10 seconds, took " + csvTime + "ms");
    }

    @Test
    void testOverdueDetectionWithManyInvoices() {
        // Create 500 invoices with various due dates
        int invoiceCount = 500;
        int overdueCount = 0;
        
        System.out.println("Creating " + invoiceCount + " invoices with various due dates...");
        
        for (int i = 0; i < invoiceCount; i++) {
            Invoice invoice = createTestInvoice(i);
            
            // Make some invoices overdue
            if (i % 3 == 0) {
                invoice.setStatus(InvoiceStatus.SENT);
                invoice.getInvoice().setDueDate(LocalDate.now().minusDays(10).toString());
                overdueCount++;
            } else if (i % 3 == 1) {
                invoice.setStatus(InvoiceStatus.VIEWED);
                invoice.getInvoice().setDueDate(LocalDate.now().minusDays(5).toString());
                overdueCount++;
            } else {
                invoice.setStatus(InvoiceStatus.SENT);
                invoice.getInvoice().setDueDate(LocalDate.now().plusDays(30).toString());
            }
            
            invoiceRepository.save(invoice);
        }
        
        System.out.println("Created " + invoiceCount + " invoices, " + overdueCount + " should be overdue");
        
        // Test overdue detection performance
        System.out.println("Running overdue detection...");
        long startOverdue = System.currentTimeMillis();
        invoiceStatusScheduler.updateOverdueInvoices();
        long endOverdue = System.currentTimeMillis();
        
        long overdueTime = endOverdue - startOverdue;
        System.out.println("Overdue detection completed in " + overdueTime + "ms");
        
        // Overdue detection should complete within reasonable time (5 seconds for 500 invoices)
        assertTrue(overdueTime < 5000, 
            "Overdue detection should complete within 5 seconds, took " + overdueTime + "ms");
        
        // Verify overdue invoices were updated
        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
        System.out.println("Found " + overdueInvoices.size() + " overdue invoices");
        assertTrue(overdueInvoices.size() > 0, "Should have found overdue invoices");
    }

    @Test
    void testDatabaseQueryPerformanceWithIndexes() {
        // Create 1000 invoices for multiple users
        int invoiceCount = 1000;
        String[] clerkIds = {"user1", "user2", "user3", "user4", "user5"};
        
        System.out.println("Creating " + invoiceCount + " invoices for " + clerkIds.length + " users...");
        
        for (int i = 0; i < invoiceCount; i++) {
            Invoice invoice = createTestInvoice(i);
            invoice.setClerkId(clerkIds[i % clerkIds.length]);
            invoice.setStatus(InvoiceStatus.values()[i % InvoiceStatus.values().length]);
            invoiceRepository.save(invoice);
        }
        
        // Test query performance with clerk ID filter (should use index)
        System.out.println("Testing query performance with clerkId filter...");
        long startQuery1 = System.currentTimeMillis();
        List<Invoice> userInvoices = invoiceRepository.findByClerkId("user1");
        long endQuery1 = System.currentTimeMillis();
        
        long query1Time = endQuery1 - startQuery1;
        System.out.println("ClerkId query returned " + userInvoices.size() + " invoices in " + query1Time + "ms");
        
        // Query should be fast with index (under 1 second)
        assertTrue(query1Time < 1000, 
            "ClerkId query should complete within 1 second, took " + query1Time + "ms");
        
        // Test query performance with clerk ID and status filter (should use compound index)
        System.out.println("Testing query performance with clerkId and status filter...");
        long startQuery2 = System.currentTimeMillis();
        List<Invoice> statusInvoices = invoiceRepository.findByClerkIdAndStatus("user1", InvoiceStatus.SENT);
        long endQuery2 = System.currentTimeMillis();
        
        long query2Time = endQuery2 - startQuery2;
        System.out.println("ClerkId+Status query returned " + statusInvoices.size() + " invoices in " + query2Time + "ms");
        
        // Query should be fast with compound index (under 1 second)
        assertTrue(query2Time < 1000, 
            "ClerkId+Status query should complete within 1 second, took " + query2Time + "ms");
    }

    @Test
    void testConcurrentWebhookProcessing() throws InterruptedException, ExecutionException {
        // Create test invoices
        int invoiceCount = 50;
        List<Invoice> invoices = new ArrayList<>();
        
        for (int i = 0; i < invoiceCount; i++) {
            Invoice invoice = createTestInvoice(i);
            invoice.setStatus(InvoiceStatus.SENT);
            invoices.add(invoiceRepository.save(invoice));
        }
        
        // Simulate concurrent webhook processing
        System.out.println("Testing concurrent processing of " + invoiceCount + " webhooks...");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        long startConcurrent = System.currentTimeMillis();
        
        for (Invoice invoice : invoices) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    // Simulate webhook processing by updating status
                    Invoice inv = invoiceRepository.findById(invoice.getId()).orElseThrow();
                    inv.setStatus(InvoiceStatus.PAID);
                    invoiceRepository.save(inv);
                    return true;
                } catch (Exception e) {
                    System.err.println("Error processing webhook: " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        long endConcurrent = System.currentTimeMillis();
        long concurrentTime = endConcurrent - startConcurrent;
        
        System.out.println("Concurrent processing completed in " + concurrentTime + "ms");
        System.out.println("Successfully processed " + successCount + " out of " + invoiceCount + " webhooks");
        
        // All webhooks should be processed successfully
        assertEquals(invoiceCount, successCount, 
            "All webhooks should be processed successfully");
        
        // Concurrent processing should complete within reasonable time (10 seconds for 50 webhooks)
        assertTrue(concurrentTime < 10000, 
            "Concurrent processing should complete within 10 seconds, took " + concurrentTime + "ms");
        
        // Verify all invoices were updated
        List<Invoice> paidInvoices = invoiceRepository.findByStatus(InvoiceStatus.PAID);
        assertEquals(invoiceCount, paidInvoices.size(), 
            "All invoices should be marked as PAID");
    }

    /**
     * Helper method to create a test invoice
     */
    private Invoice createTestInvoice(int index) {
        Invoice invoice = new Invoice();
        invoice.setClerkId(testClerkId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTransactionType(TransactionType.INTRA_STATE);
        invoice.setCompanyGSTNumber("29ABCDE1234F1Z5");
        
        // Create invoice details
        Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
        details.setNumber("INV-" + String.format("%05d", index));
        details.setDate(LocalDate.now().toString());
        details.setDueDate(LocalDate.now().plusDays(30).toString());
        invoice.setInvoice(details);
        
        // Create company details
        Invoice.Company company = new Invoice.Company();
        company.setName("Test Company " + index);
        company.setPhone("1234567890");
        company.setAddress("Test Address " + index);
        invoice.setCompany(company);
        
        // Create billing details
        Invoice.Billing billing = new Invoice.Billing();
        billing.setName("Test Customer " + index);
        billing.setPhone("0987654321");
        billing.setAddress("Customer Address " + index);
        invoice.setBilling(billing);
        
        // Create line items with GST
        List<Invoice.Item> items = new ArrayList<>();
        Invoice.Item item = new Invoice.Item();
        item.setName("Test Product " + index);
        item.setQty(10);
        item.setAmount(100.0);
        item.setGstRate(18.0);
        
        // Calculate GST for the item
        double baseAmount = item.getQty() * item.getAmount();
        GSTCalculatorService.GSTCalculation gstCalc = gstCalculatorService.calculateGST(
            baseAmount, 
            item.getGstRate(), 
            invoice.getTransactionType()
        );
        item.setCgstAmount(gstCalc.getCgst());
        item.setSgstAmount(gstCalc.getSgst());
        item.setIgstAmount(gstCalc.getIgst());
        item.setTotalWithGST(baseAmount + gstCalc.getTotal());
        
        items.add(item);
        invoice.setItems(items);
        
        // Calculate invoice-level GST totals
        Invoice.GSTDetails gstDetails = gstCalculatorService.calculateInvoiceGST(invoice);
        invoice.setGstDetails(gstDetails);
        
        return invoice;
    }
}
