package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import in.invoizo.invoicegeneratorapi.util.ValidationUtil;
import in.invoizo.invoicegeneratorapi.validator.StatusTransitionValidator;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for InvoiceService status management
 */
class InvoiceServicePropertyTest {

    private InvoiceRepository createMockRepository() {
        return mock(InvoiceRepository.class);
    }

    private StatusTransitionValidator createValidator() {
        return new StatusTransitionValidator();
    }

    private InvoiceService createService(InvoiceRepository repository, StatusTransitionValidator validator) {
        ValidationUtil validationUtil = new ValidationUtil();
        GSTCalculatorService gstCalculatorService = new GSTCalculatorService();
        return new InvoiceService(repository, validator, validationUtil, gstCalculatorService);
    }

    /**
     * Feature: invoice-enhancements, Property 1: New invoices default to Draft
     * Validates: Requirements 1.1
     * 
     * For any newly created invoice, the status field should be set to "DRAFT"
     */
    @Property(tries = 100)
    void newInvoicesDefaultToDraft(@ForAll @From("newInvoices") Invoice invoice) {
        // Given a new invoice without an ID and without a status
        assertNull(invoice.getId());
        assertNull(invoice.getStatus());

        // Create service with mocked repository
        InvoiceRepository repository = createMockRepository();
        StatusTransitionValidator validator = createValidator();
        InvoiceService service = createService(repository, validator);

        // Mock repository to return the invoice with an ID
        Invoice savedInvoice = new Invoice();
        savedInvoice.setId("generated-id");
        savedInvoice.setStatus(InvoiceStatus.DRAFT);
        savedInvoice.setClerkId(invoice.getClerkId());
        when(repository.save(any(Invoice.class))).thenReturn(savedInvoice);

        // When saving the invoice
        Invoice result = service.saveInvoice(invoice);

        // Then the status should be set to DRAFT
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(repository).save(captor.capture());
        assertEquals(InvoiceStatus.DRAFT, captor.getValue().getStatus());
    }

    /**
     * Feature: invoice-enhancements, Property 2: Status transitions preserve timestamps
     * Validates: Requirements 1.2, 1.4, 1.5
     * 
     * For any invoice status update to "SENT", "PAID", or "CANCELLED",
     * the corresponding timestamp field should be set to the current time
     */
    @Property(tries = 100)
    void statusTransitionsPreserveTimestamps(
            @ForAll @From("existingInvoices") Invoice invoice,
            @ForAll @From("timestampStatuses") InvoiceStatus newStatus) {
        
        // Given an existing invoice with a valid current status
        String clerkId = invoice.getClerkId();
        String invoiceId = invoice.getId();
        InvoiceStatus currentStatus = invoice.getStatus();

        // Create service with mocked repository
        InvoiceRepository repository = createMockRepository();
        StatusTransitionValidator validator = createValidator();
        InvoiceService service = createService(repository, validator);

        // Ensure the transition is valid
        Assume.that(validator.isValidTransition(currentStatus, newStatus));

        when(repository.findByClerkIdAndId(clerkId, invoiceId))
                .thenReturn(Optional.of(invoice));
        when(repository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeUpdate = Instant.now();

        // When updating the status
        Invoice result = service.updateStatus(clerkId, invoiceId, newStatus);

        Instant afterUpdate = Instant.now();

        // Then the appropriate timestamp should be set
        switch (newStatus) {
            case SENT:
                assertNotNull(result.getSentAt(), "sentAt should be set when status is SENT");
                assertTrue(result.getSentAt().isAfter(beforeUpdate.minusSeconds(1)) &&
                          result.getSentAt().isBefore(afterUpdate.plusSeconds(1)),
                          "sentAt should be set to current time");
                break;
            case PAID:
                assertNotNull(result.getPaidAt(), "paidAt should be set when status is PAID");
                assertTrue(result.getPaidAt().isAfter(beforeUpdate.minusSeconds(1)) &&
                          result.getPaidAt().isBefore(afterUpdate.plusSeconds(1)),
                          "paidAt should be set to current time");
                break;
            case CANCELLED:
                assertNotNull(result.getCancelledAt(), "cancelledAt should be set when status is CANCELLED");
                assertTrue(result.getCancelledAt().isAfter(beforeUpdate.minusSeconds(1)) &&
                          result.getCancelledAt().isBefore(afterUpdate.plusSeconds(1)),
                          "cancelledAt should be set to current time");
                break;
            default:
                // For other statuses, no specific timestamp is required
                break;
        }

        // Verify the status was updated
        assertEquals(newStatus, result.getStatus());
    }

    /**
     * Verify that invoices with null status are treated as DRAFT
     */
    @Property(tries = 100)
    void nullStatusDefaultsToDraft(@ForAll @From("invoicesWithNullStatus") Invoice invoice) {
        String clerkId = invoice.getClerkId();
        String invoiceId = invoice.getId();

        // Create service with mocked repository
        InvoiceRepository repository = createMockRepository();
        StatusTransitionValidator validator = createValidator();
        InvoiceService service = createService(repository, validator);

        when(repository.findByClerkIdAndId(clerkId, invoiceId))
                .thenReturn(Optional.of(invoice));
        when(repository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When updating status from null to SENT (valid transition from DRAFT)
        Invoice result = service.updateStatus(clerkId, invoiceId, InvoiceStatus.SENT);

        // Then the invoice should be treated as DRAFT and transition should succeed
        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertNotNull(result.getSentAt());
    }

    // Generator for new invoices (no ID, no status)
    @Provide
    Arbitrary<Invoice> newInvoices() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(clerkId -> {
                    Invoice invoice = new Invoice();
                    invoice.setClerkId(clerkId);
                    // Explicitly set ID and status to null
                    invoice.setId(null);
                    invoice.setStatus(null);
                    
                    // Set required nested objects
                    Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
                    details.setNumber("INV-001");
                    details.setDate("2024-01-01");
                    details.setDueDate("2024-01-31");
                    invoice.setInvoice(details);
                    
                    return invoice;
                });
    }

    // Generator for existing invoices with valid statuses
    @Provide
    Arbitrary<Invoice> existingInvoices() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30),
                Arbitraries.of(InvoiceStatus.DRAFT, InvoiceStatus.SENT, InvoiceStatus.VIEWED, InvoiceStatus.OVERDUE)
        ).as((clerkId, id, status) -> {
            Invoice invoice = new Invoice();
            invoice.setId(id);
            invoice.setClerkId(clerkId);
            invoice.setStatus(status);
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-001");
            details.setDate("2024-01-01");
            details.setDueDate("2024-01-31");
            invoice.setInvoice(details);
            
            return invoice;
        });
    }

    // Generator for invoices with null status
    @Provide
    Arbitrary<Invoice> invoicesWithNullStatus() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30)
        ).as((clerkId, id) -> {
            Invoice invoice = new Invoice();
            invoice.setId(id);
            invoice.setClerkId(clerkId);
            invoice.setStatus(null);
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-001");
            details.setDate("2024-01-01");
            details.setDueDate("2024-01-31");
            invoice.setInvoice(details);
            
            return invoice;
        });
    }

    // Generator for statuses that require timestamps
    @Provide
    Arbitrary<InvoiceStatus> timestampStatuses() {
        return Arbitraries.of(InvoiceStatus.SENT, InvoiceStatus.PAID, InvoiceStatus.CANCELLED);
    }
}
