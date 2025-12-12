package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import lombok.Data;
import org.springframework.stereotype.Service;

/**
 * Service for calculating GST (Goods and Services Tax) for invoices.
 * Handles both INTRA_STATE (CGST + SGST) and INTER_STATE (IGST) calculations.
 */
@Service
public class GSTCalculatorService {

    /**
     * Calculates GST for a single line item.
     * 
     * @param amount Base amount before GST
     * @param gstRate GST rate as a percentage (0-28)
     * @param transactionType Type of transaction (INTRA_STATE or INTER_STATE)
     * @return GSTCalculation containing breakdown of GST components
     * @throws IllegalArgumentException if GST rate is not between 0 and 28
     */
    public GSTCalculation calculateGST(double amount, double gstRate, Invoice.TransactionType transactionType) {
        validateGSTRate(gstRate);
        
        double gstAmount = (amount * gstRate) / 100.0;
        
        if (transactionType == Invoice.TransactionType.INTRA_STATE) {
            // For intra-state transactions, split GST equally between CGST and SGST
            double halfGst = gstAmount / 2.0;
            return new GSTCalculation(
                halfGst,    // CGST
                halfGst,    // SGST
                0.0,        // IGST
                gstAmount   // Total GST
            );
        } else {
            // For inter-state transactions, entire GST is IGST
            return new GSTCalculation(
                0.0,        // CGST
                0.0,        // SGST
                gstAmount,  // IGST
                gstAmount   // Total GST
            );
        }
    }

    /**
     * Calculates total GST for an entire invoice by summing GST from all line items.
     * 
     * @param invoice The invoice to calculate GST for
     * @return GSTDetails containing total GST breakdown
     */
    public Invoice.GSTDetails calculateInvoiceGST(Invoice invoice) {
        Invoice.GSTDetails gstDetails = new Invoice.GSTDetails();
        
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            gstDetails.setCgstTotal(0.0);
            gstDetails.setSgstTotal(0.0);
            gstDetails.setIgstTotal(0.0);
            gstDetails.setGstTotal(0.0);
            return gstDetails;
        }
        
        double cgstTotal = 0.0;
        double sgstTotal = 0.0;
        double igstTotal = 0.0;
        
        for (Invoice.Item item : invoice.getItems()) {
            cgstTotal += item.getCgstAmount();
            sgstTotal += item.getSgstAmount();
            igstTotal += item.getIgstAmount();
        }
        
        double gstTotal = cgstTotal + sgstTotal + igstTotal;
        
        gstDetails.setCgstTotal(cgstTotal);
        gstDetails.setSgstTotal(sgstTotal);
        gstDetails.setIgstTotal(igstTotal);
        gstDetails.setGstTotal(gstTotal);
        
        return gstDetails;
    }

    /**
     * Validates that the GST rate is within the acceptable range (0-28%).
     * 
     * @param gstRate The GST rate to validate
     * @throws IllegalArgumentException if the rate is outside the valid range
     */
    public void validateGSTRate(double gstRate) {
        if (gstRate < 0.0 || gstRate > 28.0) {
            throw new IllegalArgumentException("GST rate must be between 0 and 28 percent");
        }
    }

    /**
     * Data class to hold GST calculation results for a single line item.
     */
    @Data
    public static class GSTCalculation {
        private final double cgst;
        private final double sgst;
        private final double igst;
        private final double total;

        public GSTCalculation(double cgst, double sgst, double igst, double total) {
            this.cgst = cgst;
            this.sgst = sgst;
            this.igst = igst;
            this.total = total;
        }
    }
}
