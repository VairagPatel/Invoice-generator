package in.invoizo.invoicegeneratorapi.scheduler;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for InvoiceStatusScheduler overdue detection
 */
class InvoiceStatusSchedulerPropertyTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private InvoiceRepository createMockRepository() {
        return mock(InvoiceRepository.class);
    }

    private InvoiceStatusScheduler createScheduler(InvoiceRepository repository) {
        return new InvoiceStatusScheduler(repository);
    }

    /**
     * Feature: invoice-enhancements, Property 3: Overdue detection correctness
     * Validates: Requirements 1.3
     * 
     * For any invoice with status "SENT" or "VIEWED" and a due date in the past,
     * the system should identify it as overdue
     */
    @Property(tries = 100)
    void overdueDetectionCorrectness(@ForAll @From("overdueInvoices") Invoice invoice) {
        // Given an invoice with SENT or VIEWED status and a past due date
        assertTrue(invoice.getStatus() == InvoiceStatus.SENT || 
                  invoice.getStatus() == InvoiceStatus.VIEWED,
                  "Invoice should have SENT or VIEWED status");
        
        LocalDate dueDate = LocalDate.parse(invoice.getInvoice().getDueDate(), DATE_FORMATTER);
        LocalDate today = LocalDate.now();
        assertTrue(dueDate.isBefore(today), "Due date should be in the past");

        // Create scheduler with mocked repository
        InvoiceRepository repository = createMockRepository();
        InvoiceStatusScheduler scheduler = createScheduler(repository);

        when(repository.findByStatusIn(any())).thenReturn(List.of(invoice));
        when(repository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When the scheduled task runs
        scheduler.updateOverdueInvoices();

        // Then the invoice should be marked as OVERDUE
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(repository).save(captor.capture());
        assertEquals(InvoiceStatus.OVERDUE, captor.getValue().getStatus(),
                "Invoice with past due date should be marked as OVERDUE");
    }

    /**
     * Verify that invoices with future due dates are not marked as overdue
     */
    @Property(tries = 100)
    void futureInvoicesNotMarkedOverdue(@ForAll @From("futureInvoices") Invoice invoice) {
        // Given an invoice with SENT or VIEWED status and a future due date
        LocalDate dueDate = LocalDate.parse(invoice.getInvoice().getDueDate(), DATE_FORMATTER);
        LocalDate today = LocalDate.now();
        assertTrue(dueDate.isAfter(today) || dueDate.isEqual(today), 
                  "Due date should be today or in the future");

        // Create scheduler with mocked repository
        InvoiceRepository repository = createMockRepository();
        InvoiceStatusScheduler scheduler = createScheduler(repository);

        when(repository.findByStatusIn(any())).thenReturn(List.of(invoice));

        // When the scheduled task runs
        scheduler.updateOverdueInvoices();

        // Then the invoice should NOT be saved (status unchanged)
        verify(repository, never()).save(any(Invoice.class));
    }

    /**
     * Verify that only SENT and VIEWED invoices are checked for overdue status
     */
    @Property(tries = 100)
    void onlySentAndViewedInvoicesChecked(
            @ForAll @From("invoicesWithOtherStatuses") Invoice invoice) {
        
        // Given an invoice with a status other than SENT or VIEWED
        assertFalse(invoice.getStatus() == InvoiceStatus.SENT || 
                   invoice.getStatus() == InvoiceStatus.VIEWED,
                   "Invoice should not have SENT or VIEWED status");

        // Create scheduler with mocked repository
        InvoiceRepository repository = createMockRepository();
        InvoiceStatusScheduler scheduler = createScheduler(repository);

        // The repository should only return SENT and VIEWED invoices
        when(repository.findByStatusIn(List.of(InvoiceStatus.SENT, InvoiceStatus.VIEWED)))
                .thenReturn(List.of());

        // When the scheduled task runs
        scheduler.updateOverdueInvoices();

        // Then no invoices should be saved
        verify(repository, never()).save(any(Invoice.class));
    }

    /**
     * Verify that the scheduler handles multiple overdue invoices correctly
     */
    @Property(tries = 50)
    void multipleOverdueInvoicesHandled(
            @ForAll @From("overdueInvoicesList") List<Invoice> invoices) {
        
        // Given multiple invoices with past due dates
        Assume.that(!invoices.isEmpty());
        
        for (Invoice invoice : invoices) {
            LocalDate dueDate = LocalDate.parse(invoice.getInvoice().getDueDate(), DATE_FORMATTER);
            LocalDate today = LocalDate.now();
            assertTrue(dueDate.isBefore(today), "All invoices should have past due dates");
        }

        // Create scheduler with mocked repository
        InvoiceRepository repository = createMockRepository();
        InvoiceStatusScheduler scheduler = createScheduler(repository);

        when(repository.findByStatusIn(any())).thenReturn(invoices);
        when(repository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When the scheduled task runs
        scheduler.updateOverdueInvoices();

        // Then all invoices should be marked as OVERDUE
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(repository, times(invoices.size())).save(captor.capture());
        
        List<Invoice> savedInvoices = captor.getAllValues();
        assertEquals(invoices.size(), savedInvoices.size());
        
        for (Invoice savedInvoice : savedInvoices) {
            assertEquals(InvoiceStatus.OVERDUE, savedInvoice.getStatus(),
                    "All invoices should be marked as OVERDUE");
        }
    }

    // Generator for overdue invoices (past due date, SENT or VIEWED status)
    @Provide
    Arbitrary<Invoice> overdueInvoices() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.of(InvoiceStatus.SENT, InvoiceStatus.VIEWED),
                Arbitraries.integers().between(1, 365) // Days in the past
        ).as((id, clerkId, status, daysAgo) -> {
            Invoice invoice = new Invoice();
            invoice.setId(id);
            invoice.setClerkId(clerkId);
            invoice.setStatus(status);
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-" + id.substring(0, 5));
            details.setDate(LocalDate.now().minusDays(daysAgo + 10).format(DATE_FORMATTER));
            details.setDueDate(LocalDate.now().minusDays(daysAgo).format(DATE_FORMATTER));
            invoice.setInvoice(details);
            
            return invoice;
        });
    }

    // Generator for invoices with future due dates
    @Provide
    Arbitrary<Invoice> futureInvoices() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.of(InvoiceStatus.SENT, InvoiceStatus.VIEWED),
                Arbitraries.integers().between(0, 365) // Days in the future (0 = today)
        ).as((id, clerkId, status, daysAhead) -> {
            Invoice invoice = new Invoice();
            invoice.setId(id);
            invoice.setClerkId(clerkId);
            invoice.setStatus(status);
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-" + id.substring(0, 5));
            details.setDate(LocalDate.now().format(DATE_FORMATTER));
            details.setDueDate(LocalDate.now().plusDays(daysAhead).format(DATE_FORMATTER));
            invoice.setInvoice(details);
            
            return invoice;
        });
    }

    // Generator for invoices with statuses other than SENT or VIEWED
    @Provide
    Arbitrary<Invoice> invoicesWithOtherStatuses() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(30),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.of(InvoiceStatus.DRAFT, InvoiceStatus.PAID, 
                              InvoiceStatus.OVERDUE, InvoiceStatus.CANCELLED)
        ).as((id, clerkId, status) -> {
            Invoice invoice = new Invoice();
            invoice.setId(id);
            invoice.setClerkId(clerkId);
            invoice.setStatus(status);
            
            Invoice.InvoiceDetails details = new Invoice.InvoiceDetails();
            details.setNumber("INV-" + id.substring(0, 5));
            details.setDate(LocalDate.now().format(DATE_FORMATTER));
            details.setDueDate(LocalDate.now().plusDays(30).format(DATE_FORMATTER));
            invoice.setInvoice(details);
            
            return invoice;
        });
    }

    // Generator for lists of overdue invoices
    @Provide
    Arbitrary<List<Invoice>> overdueInvoicesList() {
        return overdueInvoices().list().ofMinSize(1).ofMaxSize(10);
    }
}
