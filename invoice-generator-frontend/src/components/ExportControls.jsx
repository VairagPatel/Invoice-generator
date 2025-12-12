import { useState } from "react";
import { FileSpreadsheet, FileText, Download } from "lucide-react";
import toast from "react-hot-toast";
import { exportInvoices } from "../service/invoiceService.js";

const ExportControls = ({ selectedInvoices, baseURL, token }) => {
  const [exporting, setExporting] = useState(false);

  const handleExport = async (format) => {
    // Validate selected invoices
    if (!selectedInvoices || selectedInvoices.length === 0) {
      toast.error("Please select at least one invoice to export");
      return;
    }

    // Debug: Log the selected invoices
    console.log("Selected invoices for export:", selectedInvoices);
    
    // Filter out any invalid invoice IDs
    const validInvoiceIds = selectedInvoices.filter(id => id && typeof id === 'string' && id.trim() !== '');
    
    console.log("Valid invoice IDs after filtering:", validInvoiceIds);
    
    if (validInvoiceIds.length === 0) {
      toast.error("No valid invoices selected for export");
      return;
    }

    if (validInvoiceIds.length !== selectedInvoices.length) {
      console.warn(`Filtered out ${selectedInvoices.length - validInvoiceIds.length} invalid invoice IDs`);
    }

    setExporting(true);
    try {
      // Get the token if it's a function
      const authToken = typeof token === 'function' ? await token() : token;
      
      if (!authToken) {
        throw new Error("Authentication token not available");
      }
      
      const response = await exportInvoices(baseURL, format, validInvoiceIds, authToken);
      
      // Create blob from response data
      const blob = new Blob([response.data], {
        type: format === 'excel' 
          ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          : 'text/csv'
      });
      
      // Generate filename with current date
      const today = new Date().toISOString().split('T')[0];
      const filename = `invoices_${today}.${format === 'excel' ? 'xlsx' : 'csv'}`;
      
      // Create download link and trigger download
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success(`Successfully exported ${validInvoiceIds.length} invoice(s) to ${format.toUpperCase()}`);
    } catch (error) {
      console.error("Export failed:", error);
      const errorMessage = error.response?.data?.message || 
                          error.message || 
                          "Failed to export invoices. Please try again.";
      toast.error(errorMessage);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="d-flex gap-2">
      <button
        className="btn btn-outline-success d-flex align-items-center gap-2"
        onClick={() => handleExport('excel')}
        disabled={exporting || !selectedInvoices || selectedInvoices.length === 0}
      >
        {exporting ? (
          <>
            <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            <span>Exporting...</span>
          </>
        ) : (
          <>
            <FileSpreadsheet size={18} />
            <span>Export to Excel</span>
          </>
        )}
      </button>
      <button
        className="btn btn-outline-success d-flex align-items-center gap-2"
        onClick={() => handleExport('csv')}
        disabled={exporting || !selectedInvoices || selectedInvoices.length === 0}
      >
        {exporting ? (
          <>
            <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            <span>Exporting...</span>
          </>
        ) : (
          <>
            <FileText size={18} />
            <span>Export to CSV</span>
          </>
        )}
      </button>
      {selectedInvoices && selectedInvoices.length > 0 && (
        <span className="d-flex align-items-center text-muted ms-2">
          <Download size={16} className="me-1" />
          {selectedInvoices.length} selected
        </span>
      )}
    </div>
  );
};

export default ExportControls;
