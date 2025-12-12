import toast from "react-hot-toast";

/**
 * Centralized error handler for the application
 * Provides user-friendly error messages and logs errors for debugging
 */

/**
 * Extract error message from various error formats
 * @param {Error|object} error - Error object from API or validation
 * @returns {string} - User-friendly error message
 */
export const getErrorMessage = (error) => {
  // If it's a validation error with a message property
  if (error?.message) {
    return error.message;
  }
  
  // If it's an axios error with response data
  if (error?.response?.data) {
    const data = error.response.data;
    
    // Check for message in response data
    if (typeof data === 'string') {
      return data;
    }
    
    if (data.message) {
      return data.message;
    }
    
    if (data.error) {
      return data.error;
    }
  }
  
  // If it's an axios error with a status
  if (error?.response?.status) {
    const status = error.response.status;
    
    switch (status) {
      case 400:
        return 'Invalid request. Please check your input and try again.';
      case 401:
        return 'Authentication required. Please log in again.';
      case 403:
        return 'Access denied. You do not have permission to perform this action.';
      case 404:
        return 'Resource not found. The requested item may have been deleted.';
      case 409:
        return 'Conflict. The operation could not be completed due to a conflict.';
      case 500:
        return 'Server error. Please try again later.';
      case 502:
        return 'Service unavailable. Please try again later.';
      case 503:
        return 'Service temporarily unavailable. Please try again later.';
      default:
        return `An error occurred (${status}). Please try again.`;
    }
  }
  
  // Network error
  if (error?.message === 'Network Error') {
    return 'Network error. Please check your internet connection.';
  }
  
  // Generic error
  return 'An unexpected error occurred. Please try again.';
};

/**
 * Handle error with toast notification and console logging
 * @param {Error|object} error - Error object
 * @param {string} context - Context where error occurred (for logging)
 * @param {string} customMessage - Optional custom message to override default
 */
export const handleError = (error, context = '', customMessage = null) => {
  const message = customMessage || getErrorMessage(error);
  
  // Log error for debugging
  console.error(`Error in ${context}:`, error);
  
  // Show toast notification
  toast.error(message);
  
  return message;
};

/**
 * Handle success with toast notification
 * @param {string} message - Success message
 */
export const handleSuccess = (message) => {
  toast.success(message);
};

/**
 * Error messages for specific operations
 */
export const ErrorMessages = {
  // Invoice operations
  INVOICE_SAVE_FAILED: 'Failed to save invoice. Please try again.',
  INVOICE_DELETE_FAILED: 'Failed to delete invoice. Please try again.',
  INVOICE_SEND_FAILED: 'Failed to send invoice. Please try again.',
  INVOICE_EXPORT_FAILED: 'Failed to export invoices. Please try again.',
  INVOICE_NOT_FOUND: 'Invoice not found. It may have been deleted.',
  
  // Status operations
  STATUS_UPDATE_FAILED: 'Failed to update status. Please try again.',
  INVALID_STATUS_TRANSITION: 'Invalid status transition. Please check the allowed transitions.',
  
  // Payment operations
  PAYMENT_LINK_FAILED: 'Failed to generate payment link. Please try again.',
  PAYMENT_PROCESSING_FAILED: 'Payment processing failed. Please try again.',
  
  // Validation errors
  INVALID_GST_RATE: 'GST rate must be between 0 and 28 percent.',
  INVALID_GST_NUMBER: 'Invalid GST number format.',
  INVALID_EMAIL: 'Invalid email address format.',
  REQUIRED_FIELD: 'This field is required.',
  
  // Export errors
  NO_INVOICES_SELECTED: 'Please select at least one invoice to export.',
  EXPORT_FORMAT_INVALID: 'Invalid export format. Please select Excel or CSV.',
  
  // Authentication errors
  AUTH_REQUIRED: 'Authentication required. Please log in.',
  AUTH_EXPIRED: 'Your session has expired. Please log in again.',
  ACCESS_DENIED: 'Access denied. You do not have permission to perform this action.',
  
  // Network errors
  NETWORK_ERROR: 'Network error. Please check your internet connection.',
  SERVER_ERROR: 'Server error. Please try again later.',
  
  // Generic
  UNEXPECTED_ERROR: 'An unexpected error occurred. Please try again.'
};

/**
 * Success messages for specific operations
 */
export const SuccessMessages = {
  INVOICE_SAVED: 'Invoice saved successfully!',
  INVOICE_DELETED: 'Invoice deleted successfully!',
  INVOICE_SENT: 'Invoice sent successfully!',
  INVOICE_EXPORTED: 'Invoices exported successfully!',
  STATUS_UPDATED: 'Status updated successfully!',
  PAYMENT_LINK_GENERATED: 'Payment link generated successfully!',
  PAYMENT_COMPLETED: 'Payment completed successfully!'
};
