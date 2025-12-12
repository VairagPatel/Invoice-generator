package in.invoizo.invoicegeneratorapi.controller;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.service.ExportService;
import in.invoizo.invoicegeneratorapi.service.InvoiceService;
import in.invoizo.invoicegeneratorapi.service.PaymentService;
import in.invoizo.invoicegeneratorapi.util.ValidationUtil;
import net.jqwik.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for InvoiceController export functionality.
 * Tests security and filename format properties.
 */
class InvoiceControllerExportPropertyTest {

    /**
     * Feature: invoice-enhancements, Property 24: Export filename format
     * For any export operation, the generated filename should match the pattern
     * "invoices_YYYY-MM-DD.(xlsx|csv)" where YYYY-MM-DD is a valid date.
     * Validates: Requirements 4.7
     */
    @Property(tries = 100)
    void exportFilenameFormat(@ForAll("exportFormats") String format) throws IOException {
        // Setup mocks
        InvoiceService invoiceService = mock(InvoiceService.class);
        ExportService exportService = mock(ExportService.class);
        Authentication authentication = mock(Authentication.class);
        
        // Create test data
        List<Invoice> testInvoices = createTestInvoices(3);
        
        when(authentication.getName()).thenReturn("test-user-123");
        when(invoiceService.fetchInvoices("test-user-123")).thenReturn(testInvoices);
        when(exportService.exportToExcel(anyList())).thenReturn(new byte[100]);
        when(exportService.exportToCSV(anyList())).thenReturn(new byte[100]);
        
        // Create controller with ValidationUtil
        ValidationUtil validationUtil = new ValidationUtil();
        PaymentService mockPaymentService = mock(PaymentService.class);
        InvoiceController controller = new InvoiceController(invoiceService, null, exportService, validationUtil, mockPaymentService);
        
        // Execute export
        ResponseEntity<byte[]> response = controller.exportInvoices(format, null, authentication);
        
        // Extract filename from Content-Disposition header
        String contentDisposition = response.getHeaders().getContentDisposition().toString();
        String filename = extractFilename(contentDisposition);
        
        // Verify filename format: invoices_YYYY-MM-DD.(xlsx|csv)
        String expectedExtension = format.equalsIgnoreCase("excel") ? "xlsx" : "csv";
        Pattern filenamePattern = Pattern.compile("invoices_\\d{4}-\\d{2}-\\d{2}\\." + expectedExtension);
        
        assertTrue(filenamePattern.matcher(filename).matches(),
                "Filename should match pattern invoices_YYYY-MM-DD." + expectedExtension + " but was: " + filename);
        
        // Verify the date part is valid
        String datePart = filename.substring(9, 19); // Extract YYYY-MM-DD
        assertDoesNotThrow(() -> LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "Date part should be a valid date");
        
        // Verify the date is today's date
        LocalDate today = LocalDate.now();
        LocalDate filenameDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(today, filenameDate,
                "Filename should contain today's date");
    }

    /**
     * Feature: invoice-enhancements, Property 25: Export data isolation
     * For any export request by a user, the exported data should only contain
     * invoices where clerkId matches the authenticated user's ID.
     * Validates: Requirements 4.9
     */
    @Property(tries = 100)
    void exportDataIsolation(
            @ForAll("userIds") String userId,
            @ForAll("exportFormats") String format
    ) throws IOException {
        // Setup mocks
        InvoiceService invoiceService = mock(InvoiceService.class);
        ExportService exportService = mock(ExportService.class);
        Authentication authentication = mock(Authentication.class);
        
        // Create test data - invoices for the authenticated user
        List<Invoice> userInvoices = createTestInvoicesForUser(userId, 5);
        
        // Create invoices for other users (should not be included)
        List<Invoice> otherUserInvoices = createTestInvoicesForUser("other-user-456", 3);
        
        when(authentication.getName()).thenReturn(userId);
        when(invoiceService.fetchInvoices(userId)).thenReturn(userInvoices);
        
        // Capture the invoices passed to export service
        List<Invoice> capturedInvoices = new ArrayList<>();
        when(exportService.exportToExcel(anyList())).thenAnswer(invocation -> {
            capturedInvoices.addAll(invocation.getArgument(0));
            return new byte[100];
        });
        when(exportService.exportToCSV(anyList())).thenAnswer(invocation -> {
            capturedInvoices.addAll(invocation.getArgument(0));
            return new byte[100];
        });
        
        // Create controller with ValidationUtil
        ValidationUtil validationUtil = new ValidationUtil();
        PaymentService mockPaymentService = mock(PaymentService.class);
        InvoiceController controller = new InvoiceController(invoiceService, null, exportService, validationUtil, mockPaymentService);
        
        // Execute export
        ResponseEntity<byte[]> response = controller.exportInvoices(format, null, authentication);
        
        // Verify response is successful
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Export should succeed");
        
        // Verify all captured invoices belong to the authenticated user
        for (Invoice invoice : capturedInvoices) {
            assertEquals(userId, invoice.getClerkId(),
                    "All exported invoices should belong to the authenticated user");
        }
        
        // Verify no invoices from other users are included
        for (Invoice otherInvoice : otherUserInvoices) {
            assertFalse(capturedInvoices.stream()
                    .anyMatch(inv -> inv.getId().equals(otherInvoice.getId())),
                    "Invoices from other users should not be included in export");
        }
    }

    /**
     * Test that export requires authentication
     */
    @Property(tries = 100)
    void exportRequiresAuthentication(@ForAll("exportFormats") String format) {
        // Setup mocks
        InvoiceService invoiceService = mock(InvoiceService.class);
        ExportService exportService = mock(ExportService.class);
        Authentication authentication = mock(Authentication.class);
        
        // Simulate unauthenticated user
        when(authentication.getName()).thenReturn(null);
        
        // Create controller with ValidationUtil
        ValidationUtil validationUtil = new ValidationUtil();
        PaymentService mockPaymentService = mock(PaymentService.class);
        InvoiceController controller = new InvoiceController(invoiceService, null, exportService, validationUtil, mockPaymentService);
        
        // Execute export and expect exception
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.exportInvoices(format, null, authentication),
                "Export should require authentication"
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode(),
                "Should return 401 Unauthorized");
        assertTrue(exception.getReason().contains("Authentication required"),
                "Error message should indicate authentication is required");
    }

    /**
     * Test that export validates format parameter
     */
    @Property(tries = 100)
    void exportValidatesFormat(@ForAll("invalidFormats") String invalidFormat) {
        // Setup mocks
        InvoiceService invoiceService = mock(InvoiceService.class);
        ExportService exportService = mock(ExportService.class);
        Authentication authentication = mock(Authentication.class);
        
        when(authentication.getName()).thenReturn("test-user-123");
        
        // Create controller with ValidationUtil
        ValidationUtil validationUtil = new ValidationUtil();
        PaymentService mockPaymentService = mock(PaymentService.class);
        InvoiceController controller = new InvoiceController(invoiceService, null, exportService, validationUtil, mockPaymentService);
        
        // Execute export with invalid format and expect exception
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.exportInvoices(invalidFormat, null, authentication),
                "Export should validate format parameter"
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode(),
                "Should return 400 Bad Request for invalid format");
        String errorMessage = exception.getReason() != null ? exception.getReason() : exception.getMessage();
        assertTrue(errorMessage != null && errorMessage.contains("Supported formats are: excel, csv"),
                "Error message should list supported formats. Got: " + errorMessage);
    }

    /**
     * Test that export handles empty invoice list
     */
    @Property(tries = 100)
    void exportHandlesEmptyInvoiceList(@ForAll("exportFormats") String format) {
        // Setup mocks
        InvoiceService invoiceService = mock(InvoiceService.class);
        ExportService exportService = mock(ExportService.class);
        Authentication authentication = mock(Authentication.class);
        
        when(authentication.getName()).thenReturn("test-user-123");
        when(invoiceService.fetchInvoices("test-user-123")).thenReturn(new ArrayList<>());
        
        // Create controller with ValidationUtil
        ValidationUtil validationUtil = new ValidationUtil();
        PaymentService mockPaymentService = mock(PaymentService.class);
        InvoiceController controller = new InvoiceController(invoiceService, null, exportService, validationUtil, mockPaymentService);
        
        // Execute export and expect exception
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.exportInvoices(format, null, authentication),
                "Export should handle empty invoice list"
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode(),
                "Should return 400 Bad Request for empty list");
        assertTrue(exception.getReason().contains("No invoices found to export"),
                "Error message should indicate no invoices found");
    }

    /**
     * Provides valid export formats for testing.
     */
    @Provide
    Arbitrary<String> exportFormats() {
        return Arbitraries.of("excel", "csv", "Excel", "CSV", "EXCEL", "Csv");
    }

    /**
     * Provides invalid export formats for testing.
     */
    @Provide
    Arbitrary<String> invalidFormats() {
        return Arbitraries.of("pdf", "json", "xml", "txt", "doc", "xls", "");
    }

    /**
     * Provides arbitrary user IDs for testing.
     */
    @Provide
    Arbitrary<String> userIds() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20);
    }

    /**
     * Helper method to create test invoices
     */
    private List<Invoice> createTestInvoices(int count) {
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Invoice invoice = new Invoice();
            invoice.setId("invoice-" + i);
            invoice.setClerkId("test-user-123");
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-" + (1000 + i));
            details.setDate("2024-01-15");
            details.setDueDate("2024-02-15");
            invoice.setInvoice(details);
            
            Invoice.Billing billing = new Invoice.Billing();
            billing.setName("Customer " + i);
            invoice.setBilling(billing);
            
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
            invoices.add(invoice);
        }
        return invoices;
    }

    /**
     * Helper method to create test invoices for a specific user
     */
    private List<Invoice> createTestInvoicesForUser(String userId, int count) {
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Invoice invoice = new Invoice();
            invoice.setId(userId + "-invoice-" + i);
            invoice.setClerkId(userId);
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-" + userId + "-" + i);
            details.setDate("2024-01-15");
            details.setDueDate("2024-02-15");
            invoice.setInvoice(details);
            
            Invoice.Billing billing = new Invoice.Billing();
            billing.setName("Customer " + i);
            invoice.setBilling(billing);
            
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
            invoices.add(invoice);
        }
        return invoices;
    }

    /**
     * Helper method to extract filename from Content-Disposition header
     */
    private String extractFilename(String contentDisposition) {
        // Content-Disposition format: "form-data; name="attachment"; filename="invoices_2024-01-15.xlsx""
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename=")) {
                return part.substring(9).replace("\"", "");
            }
        }
        return "";
    }
}
