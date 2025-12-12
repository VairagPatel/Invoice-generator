/**
 * Database Migration Script for Invoice Enhancements
 * 
 * This script migrates existing invoices to support new features:
 * - Invoice status tracking
 * - Payment integration
 * - GST tax support
 * 
 * Requirements: 5.5 - Backward compatibility with existing invoices
 */

// Migration for existing invoices without status field
db.invoices.updateMany(
    { status: { $exists: false } },
    {
        $set: {
            status: "DRAFT"
        }
    }
);

print("✓ Set status to DRAFT for " + db.invoices.countDocuments({ status: "DRAFT" }) + " invoices");

// Migration for existing invoices without transactionType field
db.invoices.updateMany(
    { transactionType: { $exists: false } },
    {
        $set: {
            transactionType: "INTRA_STATE"
        }
    }
);

print("✓ Set transactionType to INTRA_STATE for invoices");

// Migration for existing invoices without gstDetails field
db.invoices.updateMany(
    { gstDetails: { $exists: false } },
    {
        $set: {
            gstDetails: {
                cgstTotal: 0.0,
                sgstTotal: 0.0,
                igstTotal: 0.0,
                gstTotal: 0.0
            }
        }
    }
);

print("✓ Initialized GST details with zero values");

// Migration for existing invoice items without GST fields
db.invoices.updateMany(
    { "items.0": { $exists: true } },
    {
        $set: {
            "items.$[].gstRate": 0.0,
            "items.$[].cgstAmount": 0.0,
            "items.$[].sgstAmount": 0.0,
            "items.$[].igstAmount": 0.0,
            "items.$[].totalWithGST": 0.0
        }
    }
);

print("✓ Initialized GST fields for invoice items");

// Verify migration results
const totalInvoices = db.invoices.countDocuments({});
const invoicesWithStatus = db.invoices.countDocuments({ status: { $exists: true } });
const invoicesWithTransactionType = db.invoices.countDocuments({ transactionType: { $exists: true } });
const invoicesWithGstDetails = db.invoices.countDocuments({ gstDetails: { $exists: true } });

print("\n=== Migration Summary ===");
print("Total invoices: " + totalInvoices);
print("Invoices with status: " + invoicesWithStatus);
print("Invoices with transactionType: " + invoicesWithTransactionType);
print("Invoices with gstDetails: " + invoicesWithGstDetails);

if (invoicesWithStatus === totalInvoices && 
    invoicesWithTransactionType === totalInvoices && 
    invoicesWithGstDetails === totalInvoices) {
    print("\n✓ Migration completed successfully!");
} else {
    print("\n⚠ Warning: Some invoices may not have been migrated completely");
}
