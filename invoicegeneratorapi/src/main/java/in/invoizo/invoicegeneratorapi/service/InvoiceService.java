package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.exception.DatabaseConnectionException;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import in.invoizo.invoicegeneratorapi.util.RetryUtil;
import in.invoizo.invoicegeneratorapi.util.ValidationUtil;
import in.invoizo.invoicegeneratorapi.validator.StatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository repository;
    private final StatusTransitionValidator statusTransitionValidator;
    private final ValidationUtil validationUtil;
    private final GSTCalculatorService gstCalculatorService;

    public Invoice saveInvoice(Invoice invoice) {
        // Set default status for new invoices
        if (invoice.getId() == null && invoice.getStatus() == null) {
            invoice.setStatus(InvoiceStatus.DRAFT);
        }
        
        // Validate GST number if provided
        if (invoice.getCompanyGSTNumber() != null) {
            validationUtil.validateGSTNumber(invoice.getCompanyGSTNumber());
        }
        
        // Validate GST rates for all items
        if (invoice.getItems() != null) {
            for (Invoice.Item item : invoice.getItems()) {
                // GST rate is a primitive double, so it's never null
                // Just validate the value
                gstCalculatorService.validateGSTRate(item.getGstRate());
            }
        }
        
        try {
            return RetryUtil.executeWithRetry(
                () -> repository.save(invoice),
                3,
                500,
                "Save Invoice"
            );
        } catch (DataAccessException e) {
            log.error("Database error saving invoice", e);
            throw new DatabaseConnectionException("Failed to save invoice: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error saving invoice", e);
            throw new RuntimeException("Failed to save invoice: " + e.getMessage(), e);
        }
    }

    public List<Invoice> fetchInvoices(String clerkId) {
        try {
            return RetryUtil.executeWithRetry(
                () -> repository.findByClerkId(clerkId),
                3,
                500,
                "Fetch Invoices"
            );
        } catch (DataAccessException e) {
            log.error("Database error fetching invoices for clerk: {}", clerkId, e);
            throw new DatabaseConnectionException("Failed to fetch invoices: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching invoices for clerk: {}", clerkId, e);
            throw new RuntimeException("Failed to fetch invoices: " + e.getMessage(), e);
        }
    }

    public List<Invoice> fetchInvoicesByStatus(String clerkId, InvoiceStatus status) {
        try {
            return RetryUtil.executeWithRetry(
                () -> repository.findByClerkIdAndStatus(clerkId, status),
                3,
                500,
                "Fetch Invoices By Status"
            );
        } catch (DataAccessException e) {
            log.error("Database error fetching invoices by status for clerk: {}", clerkId, e);
            throw new DatabaseConnectionException("Failed to fetch invoices: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching invoices by status for clerk: {}", clerkId, e);
            throw new RuntimeException("Failed to fetch invoices: " + e.getMessage(), e);
        }
    }

    public void removeInvoice(String clerkId, String invoiceId) {
        try {
            Invoice existingInvoice = repository.findByClerkIdAndId(clerkId, invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found:" + invoiceId));
            
            RetryUtil.executeVoidWithRetry(
                () -> repository.delete(existingInvoice),
                3,
                500,
                "Delete Invoice"
            );
        } catch (DataAccessException e) {
            log.error("Database error deleting invoice: {}", invoiceId, e);
            throw new DatabaseConnectionException("Failed to delete invoice: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error deleting invoice: {}", invoiceId, e);
            throw new RuntimeException("Failed to delete invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Update the status of an invoice with validation and timestamp management
     * 
     * @param clerkId The authenticated user's clerk ID
     * @param invoiceId The invoice ID to update
     * @param newStatus The new status to set
     * @return The updated invoice
     */
    public Invoice updateStatus(String clerkId, String invoiceId, InvoiceStatus newStatus) {
        try {
            Invoice invoice = repository.findByClerkIdAndId(clerkId, invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            InvoiceStatus currentStatus = invoice.getStatus();
            
            // If status is null (old invoices), default to DRAFT
            if (currentStatus == null) {
                currentStatus = InvoiceStatus.DRAFT;
                invoice.setStatus(currentStatus);
            }

            // Validate the status transition
            statusTransitionValidator.validateTransition(currentStatus, newStatus);

            // Update status
            invoice.setStatus(newStatus);

            // Set appropriate timestamps based on new status
            Instant now = Instant.now();
            switch (newStatus) {
                case SENT:
                    invoice.setSentAt(now);
                    break;
                case PAID:
                    invoice.setPaidAt(now);
                    break;
                case CANCELLED:
                    invoice.setCancelledAt(now);
                    break;
                default:
                    // No timestamp update for other statuses
                    break;
            }

            return RetryUtil.executeWithRetry(
                () -> repository.save(invoice),
                3,
                500,
                "Update Invoice Status"
            );
            
        } catch (DataAccessException e) {
            log.error("Database error updating invoice status: {}", invoiceId, e);
            throw new DatabaseConnectionException("Failed to update invoice status: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            log.error("Unexpected error updating invoice status: {}", invoiceId, e);
            throw new RuntimeException("Failed to update invoice status: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing invoice
     * 
     * @param invoice The invoice to update
     * @return The updated invoice
     */
    public Invoice updateInvoice(Invoice invoice) {
        try {
            return RetryUtil.executeWithRetry(
                () -> repository.save(invoice),
                3,
                500,
                "Update Invoice"
            );
        } catch (DataAccessException e) {
            log.error("Database error updating invoice: {}", invoice.getId(), e);
            throw new DatabaseConnectionException("Failed to update invoice: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error updating invoice: {}", invoice.getId(), e);
            throw new RuntimeException("Failed to update invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Get invoice by ID
     * 
     * @param invoiceId The invoice ID
     * @return The invoice
     */
    public Invoice getInvoiceById(String invoiceId) {
        try {
            return RetryUtil.executeWithRetry(
                () -> repository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId)),
                3,
                500,
                "Get Invoice By ID"
            );
        } catch (DataAccessException e) {
            log.error("Database error fetching invoice: {}", invoiceId, e);
            throw new DatabaseConnectionException("Failed to fetch invoice: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            log.error("Unexpected error fetching invoice: {}", invoiceId, e);
            throw new RuntimeException("Failed to fetch invoice: " + e.getMessage(), e);
        }
    }
}
