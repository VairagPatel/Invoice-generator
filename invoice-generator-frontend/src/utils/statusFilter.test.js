import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { filterInvoicesByStatusLocal, getInvoiceStatusCounts } from './statusFilter';

/**
 * Property-Based Tests for Status Filtering
 * Feature: invoice-enhancements
 */

describe('Status Filter Property Tests', () => {
  /**
   * Feature: invoice-enhancements, Property 4: Status filter completeness
   * Validates: Requirements 1.7
   * 
   * For any collection of invoices and any status filter, all returned invoices
   * should have exactly that status, and no invoices with that status should be excluded
   */
  it('Property 4: Status filter completeness - all returned invoices match filter', () => {
    const statusArbitrary = fc.constantFrom('DRAFT', 'SENT', 'VIEWED', 'PAID', 'OVERDUE', 'CANCELLED');
    
    const invoiceArbitrary = fc.record({
      _id: fc.string(),
      title: fc.string(),
      status: fc.option(statusArbitrary, { nil: undefined })
    });
    
    fc.assert(
      fc.property(
        fc.array(invoiceArbitrary, { minLength: 0, maxLength: 100 }),
        statusArbitrary,
        (invoices, filterStatus) => {
          const filtered = filterInvoicesByStatusLocal(invoices, filterStatus);
          
          // All returned invoices should have the filter status
          const allMatch = filtered.every(invoice => {
            const invoiceStatus = invoice.status || 'DRAFT';
            return invoiceStatus === filterStatus;
          });
          expect(allMatch).toBe(true);
          
          // Count how many invoices in original array have the filter status
          const expectedCount = invoices.filter(invoice => {
            const invoiceStatus = invoice.status || 'DRAFT';
            return invoiceStatus === filterStatus;
          }).length;
          
          // Filtered array should have exactly that many invoices
          expect(filtered.length).toBe(expectedCount);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Additional test: Empty filter returns all invoices
   */
  it('Property 4a: Empty filter returns all invoices', () => {
    const statusArbitrary = fc.constantFrom('DRAFT', 'SENT', 'VIEWED', 'PAID', 'OVERDUE', 'CANCELLED');
    
    const invoiceArbitrary = fc.record({
      _id: fc.string(),
      title: fc.string(),
      status: fc.option(statusArbitrary, { nil: undefined })
    });
    
    fc.assert(
      fc.property(
        fc.array(invoiceArbitrary, { minLength: 0, maxLength: 100 }),
        (invoices) => {
          const filtered = filterInvoicesByStatusLocal(invoices, "");
          
          // Should return all invoices
          expect(filtered.length).toBe(invoices.length);
          
          // Should be the same array
          expect(filtered).toEqual(invoices);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Additional test: Status counts are accurate
   */
  it('Property 4b: Status counts match actual invoice counts', () => {
    const statusArbitrary = fc.constantFrom('DRAFT', 'SENT', 'VIEWED', 'PAID', 'OVERDUE', 'CANCELLED');
    
    const invoiceArbitrary = fc.record({
      _id: fc.string(),
      title: fc.string(),
      status: fc.option(statusArbitrary, { nil: undefined })
    });
    
    fc.assert(
      fc.property(
        fc.array(invoiceArbitrary, { minLength: 0, maxLength: 100 }),
        (invoices) => {
          const counts = getInvoiceStatusCounts(invoices);
          
          // Total count should match array length
          expect(counts.ALL).toBe(invoices.length);
          
          // Each status count should match filtered count
          const statuses = ['DRAFT', 'SENT', 'VIEWED', 'PAID', 'OVERDUE', 'CANCELLED'];
          statuses.forEach(status => {
            const expectedCount = invoices.filter(invoice => {
              const invoiceStatus = invoice.status || 'DRAFT';
              return invoiceStatus === status;
            }).length;
            
            expect(counts[status]).toBe(expectedCount);
          });
          
          // Sum of all status counts should equal total
          const sum = counts.DRAFT + counts.SENT + counts.VIEWED + 
                      counts.PAID + counts.OVERDUE + counts.CANCELLED;
          expect(sum).toBe(counts.ALL);
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Additional test: Filter preserves invoice data integrity
   */
  it('Property 4c: Filtering does not modify invoice data', () => {
    const statusArbitrary = fc.constantFrom('DRAFT', 'SENT', 'VIEWED', 'PAID', 'OVERDUE', 'CANCELLED');
    
    const invoiceArbitrary = fc.record({
      _id: fc.uuid(),  // Use UUID to ensure unique IDs
      title: fc.string(),
      status: fc.option(statusArbitrary, { nil: undefined }),
      amount: fc.double({ min: 0, max: 100000, noNaN: true })
    });
    
    fc.assert(
      fc.property(
        fc.array(invoiceArbitrary, { minLength: 1, maxLength: 100 }),
        statusArbitrary,
        (invoices, filterStatus) => {
          // Create deep copy of original invoices
          const originalInvoices = JSON.parse(JSON.stringify(invoices));
          
          // Filter invoices
          const filtered = filterInvoicesByStatusLocal(invoices, filterStatus);
          
          // Original invoices should not be modified
          expect(invoices).toEqual(originalInvoices);
          
          // Filtered invoices should have same data as originals
          filtered.forEach(filteredInvoice => {
            const original = invoices.find(inv => inv._id === filteredInvoice._id);
            expect(filteredInvoice).toEqual(original);
          });
          
          return true;
        }
      ),
      { numRuns: 100 }
    );
  });
});
