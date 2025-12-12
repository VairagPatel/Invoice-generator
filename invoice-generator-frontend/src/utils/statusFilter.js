/**
 * Filter invoices by status
 * @param {Array} invoices - Array of invoice objects
 * @param {string} status - Status to filter by (empty string means no filter)
 * @returns {Array} Filtered invoices
 */
export const filterInvoicesByStatusLocal = (invoices, status) => {
  if (!status || status === "") {
    return invoices;
  }
  
  return invoices.filter(invoice => {
    const invoiceStatus = invoice.status || "DRAFT";
    return invoiceStatus === status;
  });
};

/**
 * Get count of invoices by status
 * @param {Array} invoices - Array of invoice objects
 * @returns {Object} Object with status counts
 */
export const getInvoiceStatusCounts = (invoices) => {
  const counts = {
    ALL: invoices.length,
    DRAFT: 0,
    SENT: 0,
    VIEWED: 0,
    PAID: 0,
    OVERDUE: 0,
    CANCELLED: 0
  };
  
  invoices.forEach(invoice => {
    const status = invoice.status || "DRAFT";
    if (counts[status] !== undefined) {
      counts[status]++;
    }
  });
  
  return counts;
};
