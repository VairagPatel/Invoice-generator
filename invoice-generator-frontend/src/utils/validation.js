/**
 * Frontend validation utilities for invoice data
 */

/**
 * Validates GST rate (0-28%)
 * @param {number} rate - GST rate to validate
 * @returns {object} - { isValid: boolean, error: string }
 */
export const validateGSTRate = (rate) => {
  if (rate === null || rate === undefined || rate === '') {
    return { isValid: false, error: 'GST rate is required' };
  }
  
  const numRate = Number(rate);
  
  if (isNaN(numRate)) {
    return { isValid: false, error: 'GST rate must be a number' };
  }
  
  if (numRate < 0 || numRate > 28) {
    return { isValid: false, error: 'GST rate must be between 0 and 28 percent' };
  }
  
  return { isValid: true, error: null };
};

/**
 * Validates GST number format (Indian GST format)
 * Format: 2 digits + 5 letters + 4 digits + 1 letter + 1 alphanumeric + Z + 1 alphanumeric
 * Example: 22AAAAA0000A1Z5
 * @param {string} gstNumber - GST number to validate
 * @returns {object} - { isValid: boolean, error: string }
 */
export const validateGSTNumber = (gstNumber) => {
  // GST number is optional
  if (!gstNumber || gstNumber.trim() === '') {
    return { isValid: true, error: null };
  }
  
  const gstPattern = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/;
  
  if (!gstPattern.test(gstNumber.trim())) {
    return { 
      isValid: false, 
      error: 'Invalid GST number format. Expected format: 22AAAAA0000A1Z5' 
    };
  }
  
  return { isValid: true, error: null };
};

/**
 * Validates email address format
 * @param {string} email - Email address to validate
 * @returns {object} - { isValid: boolean, error: string }
 */
export const validateEmail = (email) => {
  if (!email || email.trim() === '') {
    return { isValid: false, error: 'Email address is required' };
  }
  
  const emailPattern = /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/;
  
  if (!emailPattern.test(email.trim())) {
    return { isValid: false, error: 'Invalid email address format' };
  }
  
  return { isValid: true, error: null };
};

/**
 * Validates status transition request
 * @param {string} currentStatus - Current invoice status
 * @param {string} newStatus - Requested new status
 * @returns {object} - { isValid: boolean, error: string }
 */
export const validateStatusTransition = (currentStatus, newStatus) => {
  const allowedTransitions = {
    'DRAFT': ['SENT', 'CANCELLED'],
    'SENT': ['VIEWED', 'PAID', 'OVERDUE', 'CANCELLED'],
    'VIEWED': ['PAID', 'OVERDUE', 'CANCELLED'],
    'OVERDUE': ['PAID', 'CANCELLED'],
    'PAID': [],
    'CANCELLED': []
  };
  
  if (!currentStatus) {
    return { isValid: false, error: 'Current status is required' };
  }
  
  if (!newStatus) {
    return { isValid: false, error: 'New status is required' };
  }
  
  const allowed = allowedTransitions[currentStatus] || [];
  
  if (!allowed.includes(newStatus)) {
    return { 
      isValid: false, 
      error: `Cannot transition from ${currentStatus} to ${newStatus}` 
    };
  }
  
  return { isValid: true, error: null };
};

/**
 * Validates required field
 * @param {any} value - Value to validate
 * @param {string} fieldName - Name of the field for error message
 * @returns {object} - { isValid: boolean, error: string }
 */
export const validateRequired = (value, fieldName) => {
  if (value === null || value === undefined || value === '') {
    return { isValid: false, error: `${fieldName} is required` };
  }
  
  if (typeof value === 'string' && value.trim() === '') {
    return { isValid: false, error: `${fieldName} is required` };
  }
  
  return { isValid: true, error: null };
};

/**
 * Validates invoice form data
 * @param {object} invoiceData - Invoice data to validate
 * @returns {object} - { isValid: boolean, errors: object }
 */
export const validateInvoiceForm = (invoiceData) => {
  const errors = {};
  
  // Validate company GST number if provided
  if (invoiceData.companyGSTNumber) {
    const gstValidation = validateGSTNumber(invoiceData.companyGSTNumber);
    if (!gstValidation.isValid) {
      errors.companyGSTNumber = gstValidation.error;
    }
  }
  
  // Validate GST rates for all items
  if (invoiceData.items && invoiceData.items.length > 0) {
    invoiceData.items.forEach((item, index) => {
      if (item.gstRate !== undefined && item.gstRate !== null) {
        const rateValidation = validateGSTRate(item.gstRate);
        if (!rateValidation.isValid) {
          errors[`item_${index}_gstRate`] = rateValidation.error;
        }
      }
    });
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};
