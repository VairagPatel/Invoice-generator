package in.invoizo.invoicegeneratorapi.migration;

import com.mongodb.client.result.UpdateResult;
import in.invoizo.invoicegeneratorapi.entity.Invoice.GSTDetails;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.entity.Invoice.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Service for migrating existing invoices to support new enhancement features.
 * 
 * This service adds default values for:
 * - status field (defaults to DRAFT)
 * - transactionType field (defaults to INTRA_STATE)
 * - gstDetails field (initialized with zero values)
 * - GST fields in items (initialized with zero values)
 * 
 * Requirements: 5.5 - Backward compatibility with existing invoices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceMigrationService {

    private final MongoTemplate mongoTemplate;

    /**
     * Run all migration steps for invoice enhancements
     * 
     * @return MigrationResult containing statistics about the migration
     */
    public MigrationResult migrateInvoices() {
        log.info("Starting invoice migration for enhancements...");
        
        MigrationResult result = new MigrationResult();
        
        // Step 1: Add status field with default value DRAFT
        result.statusMigrated = migrateStatusField();
        log.info("Migrated status field for {} invoices", result.statusMigrated);
        
        // Step 2: Add transactionType field with default value INTRA_STATE
        result.transactionTypeMigrated = migrateTransactionTypeField();
        log.info("Migrated transactionType field for {} invoices", result.transactionTypeMigrated);
        
        // Step 3: Add gstDetails field with zero values
        result.gstDetailsMigrated = migrateGstDetailsField();
        log.info("Migrated gstDetails field for {} invoices", result.gstDetailsMigrated);
        
        // Step 4: Add GST fields to items with zero values
        result.itemGstFieldsMigrated = migrateItemGstFields();
        log.info("Migrated GST fields for items in {} invoices", result.itemGstFieldsMigrated);
        
        // Verify migration
        result.totalInvoices = mongoTemplate.count(new Query(), "invoices");
        result.success = verifyMigration();
        
        if (result.success) {
            log.info("Migration completed successfully!");
        } else {
            log.warn("Migration completed with warnings. Some invoices may need manual review.");
        }
        
        return result;
    }

    /**
     * Migrate status field for invoices that don't have it
     */
    private long migrateStatusField() {
        Query query = new Query(Criteria.where("status").exists(false));
        Update update = new Update().set("status", InvoiceStatus.DRAFT.name());
        
        UpdateResult result = mongoTemplate.updateMulti(query, update, "invoices");
        return result.getModifiedCount();
    }

    /**
     * Migrate transactionType field for invoices that don't have it
     */
    private long migrateTransactionTypeField() {
        Query query = new Query(Criteria.where("transactionType").exists(false));
        Update update = new Update().set("transactionType", TransactionType.INTRA_STATE.name());
        
        UpdateResult result = mongoTemplate.updateMulti(query, update, "invoices");
        return result.getModifiedCount();
    }

    /**
     * Migrate gstDetails field for invoices that don't have it
     */
    private long migrateGstDetailsField() {
        Query query = new Query(Criteria.where("gstDetails").exists(false));
        
        GSTDetails defaultGstDetails = new GSTDetails();
        defaultGstDetails.setCgstTotal(0.0);
        defaultGstDetails.setSgstTotal(0.0);
        defaultGstDetails.setIgstTotal(0.0);
        defaultGstDetails.setGstTotal(0.0);
        
        Update update = new Update().set("gstDetails", defaultGstDetails);
        
        UpdateResult result = mongoTemplate.updateMulti(query, update, "invoices");
        return result.getModifiedCount();
    }

    /**
     * Migrate GST fields for items that don't have them
     */
    private long migrateItemGstFields() {
        // Find invoices that have items but items don't have GST fields
        Query query = new Query(Criteria.where("items").exists(true).ne(null));
        
        Update update = new Update()
            .set("items.$[].gstRate", 0.0)
            .set("items.$[].cgstAmount", 0.0)
            .set("items.$[].sgstAmount", 0.0)
            .set("items.$[].igstAmount", 0.0)
            .set("items.$[].totalWithGST", 0.0);
        
        UpdateResult result = mongoTemplate.updateMulti(query, update, "invoices");
        return result.getModifiedCount();
    }

    /**
     * Verify that all invoices have been migrated successfully
     */
    private boolean verifyMigration() {
        long totalInvoices = mongoTemplate.count(new Query(), "invoices");
        
        long invoicesWithStatus = mongoTemplate.count(
            new Query(Criteria.where("status").exists(true)), 
            "invoices"
        );
        
        long invoicesWithTransactionType = mongoTemplate.count(
            new Query(Criteria.where("transactionType").exists(true)), 
            "invoices"
        );
        
        long invoicesWithGstDetails = mongoTemplate.count(
            new Query(Criteria.where("gstDetails").exists(true)), 
            "invoices"
        );
        
        log.info("Verification: Total={}, Status={}, TransactionType={}, GstDetails={}", 
            totalInvoices, invoicesWithStatus, invoicesWithTransactionType, invoicesWithGstDetails);
        
        return invoicesWithStatus == totalInvoices && 
               invoicesWithTransactionType == totalInvoices && 
               invoicesWithGstDetails == totalInvoices;
    }

    /**
     * Result object containing migration statistics
     */
    public static class MigrationResult {
        public long totalInvoices;
        public long statusMigrated;
        public long transactionTypeMigrated;
        public long gstDetailsMigrated;
        public long itemGstFieldsMigrated;
        public boolean success;

        @Override
        public String toString() {
            return String.format(
                "MigrationResult{totalInvoices=%d, statusMigrated=%d, transactionTypeMigrated=%d, " +
                "gstDetailsMigrated=%d, itemGstFieldsMigrated=%d, success=%s}",
                totalInvoices, statusMigrated, transactionTypeMigrated, 
                gstDetailsMigrated, itemGstFieldsMigrated, success
            );
        }
    }
}
