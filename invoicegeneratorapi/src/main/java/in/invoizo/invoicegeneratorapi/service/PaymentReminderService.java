package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderService {

    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 0 9 * * *") // Run daily at 9 AM
    public void sendPaymentReminders() {
        log.info("Starting payment reminder job");
        
        List<Invoice> unpaidInvoices = invoiceRepository.findByStatusIn(
            List.of(Invoice.InvoiceStatus.SENT, Invoice.InvoiceStatus.VIEWED)
        );

        for (Invoice invoice : unpaidInvoices) {
            try {
                processPaymentReminders(invoice);
            } catch (Exception e) {
                log.error("Error processing reminders for invoice {}", invoice.getId(), e);
            }
        }
        
        log.info("Completed payment reminder job");
    }

    private void processPaymentReminders(Invoice invoice) {
        LocalDate dueDate = LocalDate.parse(invoice.getInvoice().getDueDate());
        LocalDate today = LocalDate.now();
        
        // Initialize reminders if not present
        if (invoice.getPaymentReminders() == null) {
            invoice.setPaymentReminders(new ArrayList<>());
        }

        // Check for 2 days before due date reminder
        if (today.equals(dueDate.minusDays(2))) {
            sendReminderIfNotSent(invoice, Invoice.ReminderType.TWO_DAYS_BEFORE);
        }
        
        // Check for due date reminder
        if (today.equals(dueDate)) {
            sendReminderIfNotSent(invoice, Invoice.ReminderType.DUE_DATE);
        }
        
        // Check for overdue reminder
        if (today.isAfter(dueDate)) {
            sendReminderIfNotSent(invoice, Invoice.ReminderType.OVERDUE);
            // Update invoice status to overdue
            if (invoice.getStatus() != Invoice.InvoiceStatus.OVERDUE) {
                invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
                invoiceRepository.save(invoice);
            }
        }
    }

    private void sendReminderIfNotSent(Invoice invoice, Invoice.ReminderType reminderType) {
        // Check if reminder of this type was already sent
        boolean alreadySent = invoice.getPaymentReminders().stream()
                .anyMatch(reminder -> reminder.getType() == reminderType && reminder.isSent());

        if (!alreadySent) {
            try {
                sendPaymentReminder(invoice, reminderType);
            } catch (Exception e) {
                log.error("Failed to send {} reminder for invoice {}", reminderType, invoice.getId(), e);
            }
        }
    }

    private void sendPaymentReminder(Invoice invoice, Invoice.ReminderType reminderType) throws Exception {
        String subject = generateReminderSubject(invoice, reminderType);
        String body = generateReminderBody(invoice, reminderType);
        
        // Add payment instructions to email body
        body += "\n\nPayment Options:";
        body += "\n1. Online Payment: Use the payment button in your invoice dashboard";
        body += "\n2. Cash Payment: Pay by cash and inform us once the payment is made.";
        body += "\n\nTo make an online payment, please log in to your account and use the payment button.";

        // Send email (assuming you have customer email - you might need to add this to Invoice entity)
        String customerEmail = "customer@example.com"; // Replace with actual customer email
        emailService.sendPaymentReminderEmail(customerEmail, subject, body, invoice);

        // Record the reminder
        Invoice.PaymentReminder reminder = new Invoice.PaymentReminder();
        reminder.setId(UUID.randomUUID().toString());
        reminder.setType(reminderType);
        reminder.setScheduledDate(Instant.now());
        reminder.setSentDate(Instant.now());
        reminder.setSent(true);
        reminder.setEmailSubject(subject);
        reminder.setEmailBody(body);

        invoice.getPaymentReminders().add(reminder);
        invoiceRepository.save(invoice);

        log.info("Sent {} reminder for invoice {}", reminderType, invoice.getId());
    }

    private String generateReminderSubject(Invoice invoice, Invoice.ReminderType reminderType) {
        String invoiceNumber = invoice.getInvoice().getNumber();
        
        return switch (reminderType) {
            case TWO_DAYS_BEFORE -> "Payment Reminder: Invoice #" + invoiceNumber + " - Due in 2 Days";
            case DUE_DATE -> "Payment Due Today: Invoice #" + invoiceNumber;
            case OVERDUE -> "Overdue Payment: Invoice #" + invoiceNumber;
        };
    }

    private String generateReminderBody(Invoice invoice, Invoice.ReminderType reminderType) {
        String invoiceNumber = invoice.getInvoice().getNumber();
        String dueDate = invoice.getInvoice().getDueDate();
        String companyName = invoice.getCompany().getName();
        String customerName = invoice.getBilling().getName();
        
        double totalAmount = invoice.getPaymentDetails() != null ? 
            invoice.getPaymentDetails().getTotalAmount() : 
            calculateTotalAmount(invoice);

        String greeting = "Dear " + customerName + ",\n\n";
        
        String body = switch (reminderType) {
            case TWO_DAYS_BEFORE -> greeting +
                "This is a friendly reminder that your invoice #" + invoiceNumber + 
                " from " + companyName + " is due in 2 days on " + dueDate + ".\n\n" +
                "Invoice Amount: ₹" + String.format("%.2f", totalAmount) + "\n" +
                "Due Date: " + dueDate + "\n\n" +
                "Please ensure timely payment to avoid any inconvenience.";
                
            case DUE_DATE -> greeting +
                "Your invoice #" + invoiceNumber + " from " + companyName + 
                " is due today (" + dueDate + ").\n\n" +
                "Invoice Amount: ₹" + String.format("%.2f", totalAmount) + "\n" +
                "Due Date: " + dueDate + "\n\n" +
                "Please make the payment at your earliest convenience.";
                
            case OVERDUE -> greeting +
                "Your invoice #" + invoiceNumber + " from " + companyName + 
                " was due on " + dueDate + " and is now overdue.\n\n" +
                "Invoice Amount: ₹" + String.format("%.2f", totalAmount) + "\n" +
                "Due Date: " + dueDate + "\n\n" +
                "Please make the payment immediately to avoid any late fees or service disruption.";
        };

        body += "\n\nThank you for your business!\n\nBest regards,\n" + companyName;
        return body;
    }

    private double calculateTotalAmount(Invoice invoice) {
        double subtotal = invoice.getItems().stream()
                .mapToDouble(item -> item.getQty() * item.getAmount())
                .sum();
        
        double gstTotal = 0;
        if (invoice.getGstDetails() != null) {
            gstTotal = invoice.getGstDetails().getGstTotal();
        }
        
        return subtotal + gstTotal + invoice.getTax();
    }
}