/**
 * GST Calculator utility for calculating GST amounts based on transaction type
 */

/**
 * Calculate GST for a single line item
 * @param {number} amount - Base amount (qty * rate)
 * @param {number} gstRate - GST rate percentage (0-28)
 * @param {string} transactionType - 'INTRA_STATE' or 'INTER_STATE'
 * @returns {Object} GST calculation result with cgst, sgst, igst, and total
 */
export const calculateItemGST = (amount, gstRate, transactionType) => {
  const gstAmount = (amount * gstRate) / 100;
  
  if (transactionType === 'INTRA_STATE') {
    return {
      cgstAmount: gstAmount / 2,
      sgstAmount: gstAmount / 2,
      igstAmount: 0,
      totalGST: gstAmount,
      totalWithGST: amount + gstAmount
    };
  } else {
    // INTER_STATE
    return {
      cgstAmount: 0,
      sgstAmount: 0,
      igstAmount: gstAmount,
      totalGST: gstAmount,
      totalWithGST: amount + gstAmount
    };
  }
};

/**
 * Calculate total GST for all items in an invoice
 * @param {Array} items - Array of invoice items
 * @param {string} transactionType - 'INTRA_STATE' or 'INTER_STATE'
 * @returns {Object} Total GST breakdown
 */
export const calculateInvoiceGST = (items, transactionType) => {
  let cgstTotal = 0;
  let sgstTotal = 0;
  let igstTotal = 0;
  
  items.forEach(item => {
    const baseAmount = (item.qty || 0) * (item.amount || 0);
    const gstRate = item.gstRate || 0;
    const gst = calculateItemGST(baseAmount, gstRate, transactionType);
    
    cgstTotal += gst.cgstAmount;
    sgstTotal += gst.sgstAmount;
    igstTotal += gst.igstAmount;
  });
  
  return {
    cgstTotal,
    sgstTotal,
    igstTotal,
    gstTotal: cgstTotal + sgstTotal + igstTotal
  };
};

/**
 * Validate GST rate
 * @param {number} gstRate - GST rate to validate
 * @returns {boolean} True if valid, false otherwise
 */
export const isValidGSTRate = (gstRate) => {
  return gstRate >= 0 && gstRate <= 28;
};
