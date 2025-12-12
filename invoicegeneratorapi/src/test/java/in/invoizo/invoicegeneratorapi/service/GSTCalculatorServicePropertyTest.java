package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for GSTCalculatorService.
 * Tests universal properties that should hold across all valid inputs.
 */
class GSTCalculatorServicePropertyTest {

    private final GSTCalculatorService gstCalculatorService = new GSTCalculatorService();

    /**
     * Feature: invoice-enhancements, Property 12: Intra-state GST equality
     * For any invoice with transactionType "INTRA_STATE" and any line item,
     * the cgstAmount should equal the sgstAmount, and igstAmount should be zero.
     * Validates: Requirements 3.2
     */
    @Property(tries = 100)
    void intraStateGSTEquality(
            @ForAll("amounts") double amount,
            @ForAll("gstRates") double gstRate
    ) {
        GSTCalculatorService.GSTCalculation result = gstCalculatorService.calculateGST(
                amount, gstRate, Invoice.TransactionType.INTRA_STATE
        );

        // CGST should equal SGST
        assertEquals(result.getCgst(), result.getSgst(), 0.0001,
                "For INTRA_STATE transactions, CGST should equal SGST");

        // IGST should be zero
        assertEquals(0.0, result.getIgst(), 0.0001,
                "For INTRA_STATE transactions, IGST should be zero");

        // Total should equal CGST + SGST
        assertEquals(result.getCgst() + result.getSgst(), result.getTotal(), 0.0001,
                "Total GST should equal CGST + SGST");
    }

    /**
     * Feature: invoice-enhancements, Property 13: Inter-state GST exclusivity
     * For any invoice with transactionType "INTER_STATE" and any line item,
     * the igstAmount should be non-zero (if GST rate > 0), and both cgstAmount
     * and sgstAmount should be zero.
     * Validates: Requirements 3.3
     */
    @Property(tries = 100)
    void interStateGSTExclusivity(
            @ForAll("amounts") double amount,
            @ForAll("gstRates") double gstRate
    ) {
        GSTCalculatorService.GSTCalculation result = gstCalculatorService.calculateGST(
                amount, gstRate, Invoice.TransactionType.INTER_STATE
        );

        // CGST should be zero
        assertEquals(0.0, result.getCgst(), 0.0001,
                "For INTER_STATE transactions, CGST should be zero");

        // SGST should be zero
        assertEquals(0.0, result.getSgst(), 0.0001,
                "For INTER_STATE transactions, SGST should be zero");

        // If GST rate > 0, IGST should be non-zero
        if (gstRate > 0 && amount > 0) {
            assertTrue(result.getIgst() > 0,
                    "For INTER_STATE transactions with positive rate and amount, IGST should be non-zero");
        }

        // Total should equal IGST
        assertEquals(result.getIgst(), result.getTotal(), 0.0001,
                "Total GST should equal IGST");
    }

    /**
     * Feature: invoice-enhancements, Property 15: Line item total correctness
     * For any line item, the totalWithGST should equal (qty × amount) + cgstAmount + sgstAmount + igstAmount.
     * Validates: Requirements 3.5
     */
    @Property(tries = 100)
    void lineItemTotalCorrectness(
            @ForAll("quantities") int qty,
            @ForAll("itemAmounts") double amount,
            @ForAll("gstRates") double gstRate,
            @ForAll Invoice.TransactionType transactionType
    ) {
        double baseAmount = qty * amount;
        GSTCalculatorService.GSTCalculation gstCalc = gstCalculatorService.calculateGST(
                baseAmount, gstRate, transactionType
        );

        double expectedTotal = baseAmount + gstCalc.getCgst() + gstCalc.getSgst() + gstCalc.getIgst();
        double actualTotal = baseAmount + gstCalc.getTotal();

        assertEquals(expectedTotal, actualTotal, 0.01,
                "Line item total should equal base amount plus all GST components");
    }

    /**
     * Feature: invoice-enhancements, Property 16: GST total calculation
     * For any invoice, the gstDetails.gstTotal should equal the sum of
     * gstDetails.cgstTotal + gstDetails.sgstTotal + gstDetails.igstTotal.
     * Validates: Requirements 3.10
     */
    @Property(tries = 100)
    void gstTotalCalculation(@ForAll("invoices") Invoice invoice) {
        Invoice.GSTDetails gstDetails = gstCalculatorService.calculateInvoiceGST(invoice);

        double sumOfComponents = gstDetails.getCgstTotal() + gstDetails.getSgstTotal() + gstDetails.getIgstTotal();

        assertEquals(sumOfComponents, gstDetails.getGstTotal(), 0.01,
                "GST total should equal sum of CGST, SGST, and IGST totals");
    }

    /**
     * Feature: invoice-enhancements, Property 14: GST rate validation
     * For any GST rate input, the system should accept values between 0 and 28 (inclusive)
     * and reject all other values.
     * Validates: Requirements 3.4
     */
    @Property(tries = 100)
    void gstRateValidation(@ForAll("validGSTRates") double validRate) {
        // Valid rates should not throw exception
        assertDoesNotThrow(() -> gstCalculatorService.validateGSTRate(validRate),
                "Valid GST rates (0-28) should be accepted");
    }

    @Property(tries = 100)
    void gstRateValidationRejectsInvalidRates(@ForAll("invalidGSTRates") double invalidRate) {
        // Invalid rates should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> gstCalculatorService.validateGSTRate(invalidRate),
                "Invalid GST rates should be rejected"
        );
        
        assertEquals("GST rate must be between 0 and 28 percent", exception.getMessage(),
                "Error message should be descriptive");
    }

    /**
     * Provides arbitrary amounts for testing.
     */
    @Provide
    Arbitrary<Double> amounts() {
        return Arbitraries.doubles().between(0.0, 1000000.0);
    }

    /**
     * Provides arbitrary GST rates for testing.
     */
    @Provide
    Arbitrary<Double> gstRates() {
        return Arbitraries.doubles().between(0.0, 28.0);
    }

    /**
     * Provides valid GST rates (0-28 inclusive) for testing.
     */
    @Provide
    Arbitrary<Double> validGSTRates() {
        return Arbitraries.doubles().between(0.0, 28.0);
    }

    /**
     * Provides invalid GST rates (outside 0-28 range) for testing.
     */
    @Provide
    Arbitrary<Double> invalidGSTRates() {
        return Arbitraries.oneOf(
                Arbitraries.doubles().between(-1000.0, -0.01),  // Negative rates
                Arbitraries.doubles().between(28.01, 1000.0)    // Rates above 28
        );
    }

    /**
     * Provides arbitrary quantities for testing.
     */
    @Provide
    Arbitrary<Integer> quantities() {
        return Arbitraries.integers().between(1, 1000);
    }

    /**
     * Provides arbitrary item amounts for testing.
     */
    @Provide
    Arbitrary<Double> itemAmounts() {
        return Arbitraries.doubles().between(0.01, 10000.0);
    }

    /**
     * Provides arbitrary Invoice instances for property testing.
     */
    @Provide
    Arbitrary<Invoice> invoices() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 10),  // Number of items
                Arbitraries.of(Invoice.TransactionType.class)
        ).flatAs((itemCount, transactionType) -> {
            // Create a list of items for this transaction type
            Arbitrary<Invoice.Item> itemArbitrary = Combinators.combine(
                    Arbitraries.integers().between(1, 100),
                    Arbitraries.doubles().between(1.0, 10000.0),
                    Arbitraries.doubles().between(0.0, 28.0)
            ).as((qty, amount, gstRate) -> {
                Invoice.Item item = new Invoice.Item();
                item.setQty(qty);
                item.setAmount(amount);
                item.setGstRate(gstRate);

                // Calculate GST amounts
                double baseAmount = qty * amount;
                GSTCalculatorService.GSTCalculation gstCalc = gstCalculatorService.calculateGST(
                        baseAmount, gstRate, transactionType
                );

                item.setCgstAmount(gstCalc.getCgst());
                item.setSgstAmount(gstCalc.getSgst());
                item.setIgstAmount(gstCalc.getIgst());
                item.setTotalWithGST(baseAmount + gstCalc.getTotal());

                return item;
            });

            return itemArbitrary.list().ofSize(itemCount).map(items -> {
                Invoice invoice = new Invoice();
                invoice.setTransactionType(transactionType);
                invoice.setItems(items);
                return invoice;
            });
        });
    }
}
