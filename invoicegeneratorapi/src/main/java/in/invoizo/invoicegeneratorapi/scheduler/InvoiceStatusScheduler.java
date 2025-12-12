package in.invoizo.invoicegeneratorapi.scheduler;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceStatusScheduler {

    private final InvoiceRepository invoiceRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Scheduled task to detect and update overdue invoices
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void updateOverdueInvoices() {
        log.info("Starting overdue invoice detection task");
        
        LocalDate today = LocalDate.now();
        
        // Find all invoices with SENT or VIEWED status
        List<Invoice> invoices = invoiceRepository.findByStatusIn(
                List.of(InvoiceStatus.SENT, InvoiceStatus.VIEWED)
        );
        
        int updatedCount = 0;
        
        for (Invoice invoice : invoices) {
            try {
                String dueDateStr = invoice.getInvoice().getDueDate();
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr, DATE_FORMATTER);
                    
                    // If due date is in the past, mark as overdue
                    if (dueDate.isBefore(today)) {
                        invoice.setStatus(InvoiceStatus.OVERDUE);
                        invoiceRepository.save(invoice);
                        updatedCount++;
                        log.debug("Marked invoice {} as OVERDUE (due date: {})", 
                                invoice.getId(), dueDateStr);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing invoice {} for overdue detection: {}", 
                        invoice.getId(), e.getMessage());
            }
        }
        
        log.info("Overdue invoice detection completed. Updated {} invoices to OVERDUE status", 
                updatedCount);
    }
}
