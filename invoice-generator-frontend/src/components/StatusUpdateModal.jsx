import { useState, useContext } from "react";
import { updateInvoiceStatus } from "../service/invoiceService.js";
import { AppContext } from "../context/AppContext.jsx";
import { useAuth } from "@clerk/clerk-react";
import toast from "react-hot-toast";
import { validateStatusTransition } from "../utils/validation.js";

const StatusUpdateModal = ({ invoice, onUpdate, onClose }) => {
  const [newStatus, setNewStatus] = useState(invoice.status || "DRAFT");
  const [isUpdating, setIsUpdating] = useState(false);
  const { baseURL } = useContext(AppContext);
  const { getToken } = useAuth();

  // Define allowed status transitions based on current status
  const getAllowedTransitions = (currentStatus) => {
    const transitions = {
      DRAFT: ["SENT", "CANCELLED"],
      SENT: ["VIEWED", "PAID", "OVERDUE", "CANCELLED"],
      VIEWED: ["PAID", "OVERDUE", "CANCELLED"],
      OVERDUE: ["PAID", "CANCELLED"],
      PAID: [],
      CANCELLED: []
    };
    return transitions[currentStatus] || [];
  };

  const allowedStatuses = getAllowedTransitions(invoice.status || "DRAFT");
  const allStatuses = ["DRAFT", "SENT", "VIEWED", "PAID", "OVERDUE", "CANCELLED"];

  const handleUpdate = async () => {
    if (newStatus === invoice.status) {
      toast.error("Please select a different status");
      return;
    }

    // Validate status transition
    const validation = validateStatusTransition(invoice.status || "DRAFT", newStatus);
    if (!validation.isValid) {
      toast.error(validation.error);
      return;
    }

    setIsUpdating(true);
    try {
      const token = await getToken();
      const invoiceId = invoice.id || invoice._id;
      if (!invoiceId) {
        throw new Error("Invoice ID not found");
      }
      await updateInvoiceStatus(baseURL, invoiceId, newStatus, token);
      toast.success("Status updated successfully");
      onUpdate();
      onClose();
    } catch (error) {
      console.error("Failed to update status", error);
      const errorMessage = error.response?.data?.message || "Failed to update status";
      toast.error(errorMessage);
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: "rgba(0,0,0,0.5)" }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">Update Invoice Status</h5>
            <button
              type="button"
              className="btn-close"
              onClick={onClose}
              disabled={isUpdating}
            ></button>
          </div>
          <div className="modal-body">
            <div className="mb-3">
              <label className="form-label">Current Status</label>
              <input
                type="text"
                className="form-control bg-light"
                value={invoice.status || "DRAFT"}
                readOnly
              />
            </div>
            <div className="mb-3">
              <label className="form-label">New Status</label>
              <select
                className="form-select"
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
                disabled={isUpdating}
              >
                <option value="">Select new status</option>
                {allStatuses.map((status) => {
                  const isAllowed = allowedStatuses.includes(status);
                  const isCurrent = status === invoice.status;
                  return (
                    <option
                      key={status}
                      value={status}
                      disabled={!isAllowed || isCurrent}
                    >
                      {status} {!isAllowed && !isCurrent ? "(Not allowed)" : ""}
                      {isCurrent ? "(Current)" : ""}
                    </option>
                  );
                })}
              </select>
              {allowedStatuses.length === 0 && (
                <small className="text-muted d-block mt-2">
                  No status transitions available from {invoice.status}
                </small>
              )}
            </div>
            {newStatus && newStatus !== invoice.status && (
              <div className="alert alert-info">
                <small>
                  Are you sure you want to change the status from{" "}
                  <strong>{invoice.status || "DRAFT"}</strong> to{" "}
                  <strong>{newStatus}</strong>?
                </small>
              </div>
            )}
          </div>
          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={isUpdating}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleUpdate}
              disabled={isUpdating || !newStatus || newStatus === invoice.status}
            >
              {isUpdating ? "Updating..." : "Update Status"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StatusUpdateModal;
