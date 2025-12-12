package in.invoizo.invoicegeneratorapi.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "invoices")
@CompoundIndexes({
    @CompoundIndex(name = "clerkId_status_idx", def = "{'clerkId': 1, 'status': 1}"),
    @CompoundIndex(name = "clerkId_dueDate_idx", def = "{'clerkId': 1, 'invoice.dueDate': 1}")
})
public class Invoice {
    @Id
    private String id;

    private Company company;
    private Billing billing;
    private Shipping shipping;
    private InvoiceDetails invoice;
    private List<Item> items;
    private String notes;
    private String logo;
    private double tax;
    private String clerkId;
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant lastUpdatedAt;
    private String thumbnailUrl;
    private String template;
    private String title;

    // New fields for status tracking
    private InvoiceStatus status;
    private Instant sentAt;
    private Instant paidAt;
    private Instant cancelledAt;

    // New fields for GST
    private GSTDetails gstDetails;
    private String companyGSTNumber;
    private TransactionType transactionType;

    // Payment related fields
    private PaymentDetails paymentDetails;
    private List<PaymentReminder> paymentReminders;

    @Data
    public static class Company {
        private String name;
        private String phone;
        private String address;
    }

    @Data
    public static class Billing {
        private String name;
        private String phone;
        private String address;
    }

    @Data
    public static class Shipping {
        private String name;
        private String phone;
        private String address;
    }

    @Data
    public static class InvoiceDetails {
        private String number;
        private String date;
        private String dueDate;
    }

    @Data
    public static class Item {
        private String name;
        private int qty;
        private double amount;
        private String description;
        
        // New GST calculation fields
        private double gstRate;
        private double cgstAmount;
        private double sgstAmount;
        private double igstAmount;
        private double totalWithGST;
    }

    @Data
    public static class GSTDetails {
        private double cgstTotal;
        private double sgstTotal;
        private double igstTotal;
        private double gstTotal;
    }

    public enum InvoiceStatus {
        DRAFT, SENT, VIEWED, PAID, OVERDUE, CANCELLED
    }

    public enum TransactionType {
        INTRA_STATE, INTER_STATE
    }

    @Data
    public static class PaymentDetails {
        // Removed Razorpay-specific fields - using cash only
        private PaymentMethod paymentMethod;
        private PaymentStatus paymentStatus;
        private double totalAmount;
        private String currency;
        private Instant paymentDate;
        private String paymentLink;
        private boolean cashPaymentAllowed;
    }

    @Data
    public static class PaymentReminder {
        private String id;
        private ReminderType type;
        private Instant scheduledDate;
        private Instant sentDate;
        private boolean sent;
        private String emailSubject;
        private String emailBody;
    }

    public enum PaymentMethod {
        CASH, BANK_TRANSFER
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }

    public enum ReminderType {
        TWO_DAYS_BEFORE, DUE_DATE, OVERDUE
    }
}