import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { calculateItemGST, calculateInvoiceGST, isValidGSTRate } from './gstCalculator';

/**
 * Property-Based Tests for GST Calculator
 * Feature: invoice-enhancements
 */

describe('GST Calculator Property Tests', () => {
  /**
   * Feature: invoice-enhancements, Property 12: Intra-state GST equality
   * Validates: Requirements 3.2
   * 
   * For any invoice with transactionType "INTRA_STATE" and any line item,
   * the cgstAmount should equal the sgstAmount, and igstAmount should be zero
   */
  it('Property 12: Intra-state GST equality - CGST equals SGST and IGST is zero', () => {
    fc.assert(
      fc.property(
        fc.double({ min: 0, max: 1000000, noNaN: true }),  // amount
        fc.double({ min: 0, max: 28, noNaN: true }),       // gstRate
        (amount, gstRate) => {
          const result = calculateItemGST(amount, gstRate, 'INTRA_STATE');
          
          // CGST should equal SGST
          expect(Math.abs(result.cgstAmount - result.sgstAmount)).toBeLessThan(0.0001);
          
          // IGST should be zero
          expect(result.igstAmount).toBe(0);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Feature: invoice-enhancements, Property 13: Inter-state GST exclusivity
   * Validates: Requirements 3.3
   * 
   * For any invoice with transactionType "INTER_STATE" and any line item,
   * the igstAmount should be non-zero (if GST rate > 0), and both cgstAmount
   * and sgstAmount should be zero
   */
  it('Property 13: Inter-state GST exclusivity - IGST only, no CGST/SGST', () => {
    fc.assert(
      fc.property(
        fc.double({ min: 0.01, max: 1000000, noNaN: true }),  // amount > 0 to avoid underflow
        fc.double({ min: 0.01, max: 28, noNaN: true }),       // gstRate > 0
        (amount, gstRate) => {
          const result = calculateItemGST(amount, gstRate, 'INTER_STATE');
          
          // CGST should be zero
          expect(result.cgstAmount).toBe(0);
          
          // SGST should be zero
          expect(result.sgstAmount).toBe(0);
          
          // IGST should be non-zero when both rate and amount are > 0
          expect(result.igstAmount).toBeGreaterThan(0);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Feature: invoice-enhancements, Property 15: Line item total correctness
   * Validates: Requirements 3.5
   * 
   * For any line item, the totalWithGST should equal
   * (qty × amount) + cgstAmount + sgstAmount + igstAmount
   */
  it('Property 15: Line item total correctness - total equals base plus all GST components', () => {
    fc.assert(
      fc.property(
        fc.double({ min: 0, max: 1000000, noNaN: true }),  // amount
        fc.double({ min: 0, max: 28, noNaN: true }),       // gstRate
        fc.constantFrom('INTRA_STATE', 'INTER_STATE'),     // transactionType
        (amount, gstRate, transactionType) => {
          const result = calculateItemGST(amount, gstRate, transactionType);
          
          // Calculate expected total
          const expectedTotal = amount + result.cgstAmount + result.sgstAmount + result.igstAmount;
          
          // Verify totalWithGST matches
          expect(Math.abs(result.totalWithGST - expectedTotal)).toBeLessThan(0.0001);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Additional property test: GST total calculation
   * Validates: Requirements 3.10
   * 
   * For any invoice, the total GST should equal the sum of CGST + SGST + IGST
   */
  it('Property 16: GST total calculation - gstTotal equals sum of components', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            qty: fc.integer({ min: 1, max: 100 }),
            amount: fc.double({ min: 0, max: 10000, noNaN: true }),
            gstRate: fc.double({ min: 0, max: 28, noNaN: true })
          }),
          { minLength: 1, maxLength: 20 }
        ),
        fc.constantFrom('INTRA_STATE', 'INTER_STATE'),
        (items, transactionType) => {
          const result = calculateInvoiceGST(items, transactionType);
          
          // GST total should equal sum of components
          const expectedTotal = result.cgstTotal + result.sgstTotal + result.igstTotal;
          expect(Math.abs(result.gstTotal - expectedTotal)).toBeLessThan(0.0001);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Additional property test: GST rate validation
   * Validates: Requirements 3.4
   * 
   * For any GST rate input, the system should accept values between 0 and 28
   * (inclusive) and reject all other values
   */
  it('Property 14: GST rate validation - accepts 0-28, rejects others', () => {
    fc.assert(
      fc.property(
        fc.double({ min: 0, max: 28, noNaN: true }),
        (gstRate) => {
          // Valid rates should be accepted
          expect(isValidGSTRate(gstRate)).toBe(true);
          return true;
        }
      ),
      { numRuns: 100 }
    );

    fc.assert(
      fc.property(
        fc.oneof(
          fc.double({ min: -1000, max: -0.01, noNaN: true }),
          fc.double({ min: 28.01, max: 1000, noNaN: true })
        ),
        (gstRate) => {
          // Invalid rates should be rejected
          expect(isValidGSTRate(gstRate)).toBe(false);
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });
});
