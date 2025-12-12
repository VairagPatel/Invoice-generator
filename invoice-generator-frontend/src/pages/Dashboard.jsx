import { useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Filter, CreditCard, Clock } from "lucide-react";
import { AppContext, initialInvoiceData } from "../context/AppContext.jsx";
import { getAllInvoices, filterInvoicesByStatus } from "../service/invoiceService.js";
import toast from "react-hot-toast";
import { useAuth } from "@clerk/clerk-react";
import StatusBadge from "../components/StatusBadge.jsx";
import StatusUpdateModal from "../components/StatusUpdateModal.jsx";

import PaymentButton from "../components/PaymentButton.jsx";
import ExportControls from "../components/ExportControls.jsx";

// Helper function to format date
const formatDate = (dateString) => {
  if (!dateString) return "N/A";
  const date = new Date(dateString);
  return date.toLocaleDateString();
};

// Helper function to format timestamp
const formatTimestamp = (timestamp) => {
  if (!timestamp) return "N/A";
  const date = new Date(timestamp);
  return date.toLocaleString();
};

function Dashboard() {
  const [invoices, setInvoices] = useState([]);
  const [selectedStatus, setSelectedStatus] = useState("");
  const [showStatusModal, setShowStatusModal] = useState(false);

  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [selectedInvoiceIds, setSelectedInvoiceIds] = useState([]);
  const navigate = useNavigate();
  const { baseURL, setInvoiceData, setSelectedTemplate, setInvoiceTitle } =
    useContext(AppContext);

  const { getToken } = useAuth();

  const fetchInvoices = async (statusFilter = "") => {
    try {
      const token = await getToken();
      let response;
      if (statusFilter) {
        response = await filterInvoicesByStatus(baseURL, statusFilter, token);
      } else {
        response = await getAllInvoices(baseURL, token);
      }
      setInvoices(response.data);
    } catch (error) {
      console.error("Failed to load invoices", error);
      toast.error("Something went wrong. Unable to load invoices");
    }
  };

  useEffect(() => {
    fetchInvoices(selectedStatus);
    // Clear selection when filter changes
    setSelectedInvoiceIds([]);
  }, [baseURL, selectedStatus]);

  const handleViewClick = (invoice) => {
    setInvoiceData(invoice);
    setSelectedTemplate(invoice.template || "template1");
    setInvoiceTitle(invoice.title || "View Invoice");
    navigate("/preview");
  };

  const handleCreateNew = () => {
    // Reset to initial state from context if needed
    setInvoiceTitle("Create Invoice");
    setSelectedTemplate("template1");
    setInvoiceData(initialInvoiceData);
    navigate("/generate");
  };

  const handleStatusClick = (e, invoice) => {
    e.stopPropagation();
    setSelectedInvoice(invoice);
    setShowStatusModal(true);
  };

  const handleStatusUpdate = () => {
    fetchInvoices(selectedStatus);
  };



  const handlePaymentSuccess = () => {
    fetchInvoices(selectedStatus);
  };

  const handleSelectInvoice = (invoiceId, isChecked) => {
    if (isChecked) {
      setSelectedInvoiceIds(prev => [...prev, invoiceId]);
    } else {
      setSelectedInvoiceIds(prev => prev.filter(id => id !== invoiceId));
    }
  };

  const handleSelectAll = (isChecked) => {
    if (isChecked) {
      // Use id field (Spring Boot) or _id field (MongoDB) - whichever exists
      const allIds = invoices.map(invoice => invoice.id || invoice._id);
      setSelectedInvoiceIds(allIds);
    } else {
      setSelectedInvoiceIds([]);
    }
  };

  const isAllSelected = invoices.length > 0 && selectedInvoiceIds.length === invoices.length;

  const getStatusCounts = () => {
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

  const statusCounts = getStatusCounts();

  return (
    <div className="container py-5">
      {/* Export Controls Section */}
      {invoices.length > 0 && (
        <div className="mb-4 d-flex justify-content-between align-items-center">
          <div className="form-check">
            <input
              className="form-check-input"
              type="checkbox"
              id="selectAll"
              checked={isAllSelected}
              onChange={(e) => handleSelectAll(e.target.checked)}
            />
            <label className="form-check-label" htmlFor="selectAll">
              Select All ({invoices.length})
            </label>
          </div>
          <ExportControls 
            selectedInvoices={selectedInvoiceIds} 
            baseURL={baseURL}
            token={getToken}
          />
        </div>
      )}

      {/* Status Filter Section */}
      <div className="mb-4">
        <div className="d-flex align-items-center gap-3 mb-3">
          <Filter size={20} />
          <h5 className="mb-0">Filter by Status</h5>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <button
            className={`btn ${selectedStatus === "" ? "btn-primary" : "btn-outline-primary"}`}
            onClick={() => setSelectedStatus("")}
          >
            All ({statusCounts.ALL})
          </button>
          <button
            className={`btn ${selectedStatus === "DRAFT" ? "btn-secondary" : "btn-outline-secondary"}`}
            onClick={() => setSelectedStatus("DRAFT")}
          >
            Draft ({statusCounts.DRAFT})
          </button>
          <button
            className={`btn ${selectedStatus === "SENT" ? "btn-info" : "btn-outline-info"}`}
            onClick={() => setSelectedStatus("SENT")}
          >
            Sent ({statusCounts.SENT})
          </button>
          <button
            className={`btn ${selectedStatus === "VIEWED" ? "btn-primary" : "btn-outline-primary"}`}
            onClick={() => setSelectedStatus("VIEWED")}
          >
            Viewed ({statusCounts.VIEWED})
          </button>
          <button
            className={`btn ${selectedStatus === "PAID" ? "btn-success" : "btn-outline-success"}`}
            onClick={() => setSelectedStatus("PAID")}
          >
            Paid ({statusCounts.PAID})
          </button>
          <button
            className={`btn ${selectedStatus === "OVERDUE" ? "btn-danger" : "btn-outline-danger"}`}
            onClick={() => setSelectedStatus("OVERDUE")}
          >
            Overdue ({statusCounts.OVERDUE})
          </button>
          <button
            className={`btn ${selectedStatus === "CANCELLED" ? "btn-dark" : "btn-outline-dark"}`}
            onClick={() => setSelectedStatus("CANCELLED")}
          >
            Cancelled ({statusCounts.CANCELLED})
          </button>
        </div>
      </div>

      <div className="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-5 g-4">
        {/* Create New Invoice Card */}
        <div className="col">
          <div
            className="card h-100 d-flex justify-content-center align-items-center border border-2 border-light shadow-sm"
            style={{ cursor: "pointer", minHeight: "270px" }}
            onClick={handleCreateNew}
          >
            <Plus size={48} />
            <p className="mt-3 fw-medium">Create New Invoice</p>
          </div>
        </div>

        {/* Render Existing Invoices */}
        {invoices.map((invoice, idx) => (
          <div key={idx} className="col">
            <div
              className="card h-100 shadow-sm position-relative"
              style={{ cursor: "pointer", minHeight: "270px" }}
              onClick={() => handleViewClick(invoice)}
            >
              {/* Selection Checkbox */}
              <div 
                className="position-absolute top-0 start-0 m-2" 
                style={{ zIndex: 10 }}
                onClick={(e) => e.stopPropagation()}
              >
                <input
                  className="form-check-input"
                  type="checkbox"
                  checked={selectedInvoiceIds.includes(invoice.id || invoice._id)}
                  onChange={(e) => handleSelectInvoice(invoice.id || invoice._id, e.target.checked)}
                  style={{ width: "20px", height: "20px", cursor: "pointer" }}
                />
              </div>
              
              {invoice.thumbnailUrl && (
                <img
                  src={invoice.thumbnailUrl}
                  className="card-img-top"
                  alt="Invoice Thumbnail"
                  style={{ height: "200px", objectFit: "cover" }}
                />
              )}
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start mb-2">
                  <h6 className="card-title mb-0">{invoice.title}</h6>
                  <div className="d-flex gap-1">
                    <div onClick={(e) => handleStatusClick(e, invoice)}>
                      <StatusBadge status={invoice.status || "DRAFT"} />
                    </div>
                    {/* Cash Payment Button - Show for SENT, VIEWED, or OVERDUE invoices */}
                    {(invoice.status === "SENT" || invoice.status === "VIEWED" || invoice.status === "OVERDUE") && (
                      <div onClick={(e) => e.stopPropagation()}>
                        <PaymentButton 
                          invoice={invoice} 
                          size="sm" 
                          variant="outline-success"
                          onPaymentSuccess={handlePaymentSuccess}
                        />
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Payment Status Indicators */}
                {invoice.paymentDetails && (
                  <div className="mb-2">
                    <div className="d-flex align-items-center gap-1 mb-1">
                      <CreditCard size={14} className="text-muted" />
                      <span className="small">
                        <span
                          className={`badge badge-sm ${
                            invoice.paymentDetails?.paymentStatus === "PAID" || invoice.status === "PAID"
                              ? "bg-success"
                              : invoice.paymentDetails?.paymentStatus === "FAILED"
                              ? "bg-danger"
                              : "bg-warning text-dark"
                          }`}
                        >
                          {invoice.paymentDetails?.paymentStatus || "PENDING"}
                        </span>
                      </span>
                    </div>
                    
                    {/* Show payment method when paid */}
                    {(invoice.paymentDetails?.paymentStatus === "PAID" || invoice.status === "PAID") && 
                     invoice.paymentDetails?.paymentMethod && (
                      <div className="small text-muted text-capitalize">
                        via {invoice.paymentDetails.paymentMethod.toLowerCase().replace('_', ' ')}
                      </div>
                    )}
                    
                    {/* Show payment link if available and not paid */}
                    {invoice.paymentDetails?.paymentLink && 
                     invoice.paymentDetails?.paymentStatus !== "PAID" && 
                     invoice.status !== "PAID" && (
                      <div className="small text-info">
                        Payment link available
                      </div>
                    )}
                    
                    {/* Show payment received timestamp when paid */}
                    {(invoice.paymentDetails?.paymentStatus === "PAID" || invoice.status === "PAID") && 
                     invoice.paymentDetails?.paymentDate && (
                      <div className="d-flex align-items-center gap-1 small text-muted">
                        <Clock size={12} />
                        <span>{formatTimestamp(invoice.paymentDetails.paymentDate)}</span>
                      </div>
                    )}
                  </div>
                )}
                
                <small className="text-muted">
                  Last Updated: {formatDate(invoice.lastUpdatedAt)}
                </small>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Status Update Modal */}
      {showStatusModal && selectedInvoice && (
        <StatusUpdateModal
          invoice={selectedInvoice}
          onUpdate={handleStatusUpdate}
          onClose={() => {
            setShowStatusModal(false);
            setSelectedInvoice(null);
          }}
        />
      )}


    </div>
  );
}

export default Dashboard;
