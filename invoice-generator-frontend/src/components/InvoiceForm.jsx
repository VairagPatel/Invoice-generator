import { Trash2, Upload, Building2, FileText, Users, CreditCard, Calculator } from "lucide-react";
import { assets } from "../assets/assets.js";
import { useContext, useEffect, useState } from "react";
import { AppContext } from "../context/AppContext.jsx";
import { calculateItemGST } from "../utils/gstCalculator.js";
import { validateGSTRate, validateGSTNumber } from "../utils/validation.js";
import "./InvoiceForm.css";

const InvoiceForm = () => {
  const { invoiceData, setInvoiceData } = useContext(AppContext);
  const [validationErrors, setValidationErrors] = useState({});
  
  const handleChange = (section, field, value) => {
    setInvoiceData((prev) => ({
      ...prev,
      [section]: { ...prev[section], [field]: value },
    }));
  };

  const handleItemChange = (index, field, value) => {
    const items = [...invoiceData.items];
    items[index][field] = value;
    
    // Recalculate totals when qty, amount, or gstRate changes
    if (field === "qty" || field === "amount" || field === "gstRate") {
      const baseAmount = (items[index].qty || 0) * (items[index].amount || 0);
      items[index].total = baseAmount;
      
      // Calculate GST if gstRate is set
      const gstRate = items[index].gstRate || 0;
      if (gstRate > 0) {
        const gstResult = calculateItemGST(
          baseAmount, 
          gstRate, 
          invoiceData.transactionType || 'INTRA_STATE'
        );
        items[index].cgstAmount = gstResult.cgstAmount;
        items[index].sgstAmount = gstResult.sgstAmount;
        items[index].igstAmount = gstResult.igstAmount;
        items[index].totalWithGST = gstResult.totalWithGST;
      } else {
        items[index].cgstAmount = 0;
        items[index].sgstAmount = 0;
        items[index].igstAmount = 0;
        items[index].totalWithGST = baseAmount;
      }
    }
    
    setInvoiceData((prev) => ({ ...prev, items }));
  };

  const addItem = () => {
    setInvoiceData((prev) => ({
      ...prev,
      items: [
        ...prev.items,
        { 
          name: "", 
          qty: 1, 
          amount: 0, 
          total: 0, 
          description: "",
          gstRate: 0,
          cgstAmount: 0,
          sgstAmount: 0,
          igstAmount: 0,
          totalWithGST: 0
        },
      ],
    }));
  };

  const deleteItem = (index) => {
    const items = invoiceData.items.filter((_, i) => i !== index);
    setInvoiceData((prev) => ({ ...prev, items }));
  };

  const handleSameAsBilling = () => {
    setInvoiceData((prev) => ({
      ...prev,
      shipping: { ...prev.billing },
    }));
  };

  const handleTransactionTypeChange = (newType) => {
    // Recalculate GST for all items with the new transaction type
    const updatedItems = invoiceData.items.map(item => {
      const baseAmount = (item.qty || 0) * (item.amount || 0);
      const gstRate = item.gstRate || 0;
      
      if (gstRate > 0) {
        const gstResult = calculateItemGST(baseAmount, gstRate, newType);
        return {
          ...item,
          cgstAmount: gstResult.cgstAmount,
          sgstAmount: gstResult.sgstAmount,
          igstAmount: gstResult.igstAmount,
          totalWithGST: gstResult.totalWithGST
        };
      }
      return item;
    });
    
    setInvoiceData((prev) => ({
      ...prev,
      transactionType: newType,
      items: updatedItems
    }));
  };

  const calculateTotals = () => {
    const subtotal = invoiceData.items.reduce(
      (sum, item) => sum + (item.total || 0),
      0
    );
    
    // Calculate GST totals
    const cgstTotal = invoiceData.items.reduce(
      (sum, item) => sum + (item.cgstAmount || 0),
      0
    );
    const sgstTotal = invoiceData.items.reduce(
      (sum, item) => sum + (item.sgstAmount || 0),
      0
    );
    const igstTotal = invoiceData.items.reduce(
      (sum, item) => sum + (item.igstAmount || 0),
      0
    );
    const gstTotal = cgstTotal + sgstTotal + igstTotal;
    
    // Legacy tax calculation (if tax field is used)
    const taxRate = Number(invoiceData.tax || 0);
    const taxAmount = (subtotal * taxRate) / 100;
    
    const grandTotal = subtotal + gstTotal + taxAmount;
    
    return { 
      subtotal, 
      taxAmount, 
      cgstTotal, 
      sgstTotal, 
      igstTotal, 
      gstTotal,
      grandTotal 
    };
  };

  const { subtotal, taxAmount, cgstTotal, sgstTotal, igstTotal, gstTotal, grandTotal } = calculateTotals();

  const handleLogoUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setInvoiceData((prev) => ({
          ...prev,
          logo: reader.result, // base64 string
        }));
      };
      reader.readAsDataURL(file);
    }
  };

  useEffect(() => {
    // Only generate if it's not already set (for example, editing an existing invoice)
    if (!invoiceData.invoice.number) {
      const randomNumber = `INV-${Math.floor(100000 + Math.random() * 900000)}`;
      setInvoiceData((prev) => ({
        ...prev,
        invoice: { ...prev.invoice, number: randomNumber },
      }));
    }
  }, []);

  return (
    <div className="invoiceform container py-4">
      {/* COMPANY LOGO */}
      <div className="mb-4">
        <h5><Upload size={20} /> Company Logo</h5>
        <div className="d-flex align-items-center gap-3">
          <label htmlFor="image" className="form-label logo-upload-area">
            <img
              src={invoiceData.logo ? invoiceData.logo : assets.upload_area}
              alt=""
              width={98}
            />
          </label>
          <input
            type="file"
            className="form-control"
            name="logo"
            id="image"
            hidden
            accept="image/*"
            onChange={handleLogoUpload}
          />
        </div>
      </div>
      {/* COMPANY INFO */}
      <div className="mb-4">
        <h5><Building2 size={20} /> Your Company</h5>
        <div className="row g-3">
          <div className="col-md-6">
            <input
              type="text"
              className="form-control"
              placeholder="Company Name"
              value={invoiceData.company.name}
              onChange={(e) => handleChange("company", "name", e.target.value)}
            />
          </div>
          <div className="col-md-6">
            <input
              type="text"
              className="form-control"
              placeholder="Company Phone"
              value={invoiceData.company.phone}
              onChange={(e) => handleChange("company", "phone", e.target.value)}
            />
          </div>
          <div className="col-12">
            <input
              type="text"
              className="form-control"
              placeholder="Company Address"
              value={invoiceData.company.address}
              onChange={(e) =>
                handleChange("company", "address", e.target.value)
              }
            />
          </div>
        </div>
      </div>

      {/* GST INFORMATION */}
      <div className="mb-4 gst-info-section">
        <h5><Calculator size={20} /> GST Information</h5>
        <div className="gst-info-card">
          <div className="row g-3">
            <div className="col-md-6">
              <label className="form-label">Company GST Number (Optional)</label>
              <input
                type="text"
                className={`form-control ${validationErrors.companyGSTNumber ? 'is-invalid' : ''}`}
                placeholder="e.g., 22AAAAA0000A1Z5"
                value={invoiceData.companyGSTNumber || ""}
                onChange={(e) => {
                  const value = e.target.value;
                  setInvoiceData((prev) => ({ ...prev, companyGSTNumber: value }));
                  
                  // Validate GST number
                  const validation = validateGSTNumber(value);
                  if (!validation.isValid) {
                    setValidationErrors(prev => ({ ...prev, companyGSTNumber: validation.error }));
                  } else {
                    setValidationErrors(prev => {
                      // eslint-disable-next-line no-unused-vars
                      const { companyGSTNumber, ...rest } = prev;
                      return rest;
                    });
                  }
                }}
              />
              {validationErrors.companyGSTNumber && (
                <div className="invalid-feedback d-block">
                  {validationErrors.companyGSTNumber}
                </div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label">Transaction Type</label>
              <select
                className="form-select"
                value={invoiceData.transactionType || "INTRA_STATE"}
                onChange={(e) => handleTransactionTypeChange(e.target.value)}
              >
                <option value="INTRA_STATE">🏢 Intra-State (CGST + SGST)</option>
                <option value="INTER_STATE">🌍 Inter-State (IGST)</option>
              </select>
              <small className="text-muted mt-1 d-block">
                {invoiceData.transactionType === 'INTRA_STATE' 
                  ? 'Same state: GST split into CGST & SGST' 
                  : 'Different states: Single IGST applied'}
              </small>
            </div>
          </div>
        </div>
      </div>

      {/* BILL TO */}
      <div className="mb-4">
        <h5><Users size={20} /> Bill To</h5>
        <div className="row g-3">
          <div className="col-md-6">
            <input
              type="text"
              className="form-control"
              placeholder="Name"
              value={invoiceData.billing.name}
              onChange={(e) => handleChange("billing", "name", e.target.value)}
            />
          </div>
          <div className="col-md-6">
            <input
              type="text"
              className="form-control"
              placeholder="Phone"
              value={invoiceData.billing.phone}
              onChange={(e) => handleChange("billing", "phone", e.target.value)}
            />
          </div>
          <div className="col-12">
            <input
              type="text"
              className="form-control"
              placeholder="Address"
              value={invoiceData.billing.address}
              onChange={(e) =>
                handleChange("billing", "address", e.target.value)
              }
            />
          </div>
        </div>
      </div>

      {/* SHIP TO */}
      <div className="mb-4">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <h5>Ship To</h5>
          <div className="form-check">
            <input
              type="checkbox"
              className="form-check-input"
              id="sameAsBilling"
              onChange={handleSameAsBilling}
            />
            <label className="form-check-label" htmlFor="sameAsBilling">
              Same as Bill To
            </label>
          </div>
        </div>
        <div className="row g-3">
          <div className="col-md-6">
            <input
              type="text"
              className="form-control"
              placeholder="Name"
              value={invoiceData.shipping.name}
              onChange={(e) => handleChange("shipping", "name", e.target.value)}
            />
          </div>
          <div className="col-md-6">
            <input
              type="text"
              className="form-control"
              placeholder="Phone"
              value={invoiceData.shipping.phone}
              onChange={(e) =>
                handleChange("shipping", "phone", e.target.value)
              }
            />
          </div>
          <div className="col-12">
            <input
              type="text"
              className="form-control"
              placeholder="Address"
              value={invoiceData.shipping.address}
              onChange={(e) =>
                handleChange("shipping", "address", e.target.value)
              }
            />
          </div>
        </div>
      </div>

      {/* INVOICE INFO */}
      <div className="mb-4">
        <h5><FileText size={20} /> Invoice Information</h5>
        <div className="row g-3">
          <div className="col-md-4">
            <label className="form-label">Invoice Number</label>
            <input
              disabled
              type="text"
              className="form-control"
              value={invoiceData.invoice.number}
              onChange={(e) =>
                handleChange("invoice", "number", e.target.value)
              }
            />
          </div>
          <div className="col-md-4">
            <label className="form-label">Invoice Date</label>
            <input
              type="date"
              className="form-control"
              placeholder="Invoice Date"
              value={invoiceData.invoice.date}
              onChange={(e) => handleChange("invoice", "date", e.target.value)}
            />
          </div>
          <div className="col-md-4">
            <label className="form-label">Invoice Due Date</label>
            <input
              type="date"
              className="form-control"
              placeholder="Due Date"
              value={invoiceData.invoice.dueDate}
              onChange={(e) =>
                handleChange("invoice", "dueDate", e.target.value)
              }
            />
          </div>
        </div>
      </div>

      {/* ITEM DETAILS */}
      <div className="mb-4">
        <h5>📦 Item Details</h5>
        {invoiceData.items.map((item, index) => (
          <div key={index} className="card p-3 mb-3">
            <div className="item-card-header">
              Item #{index + 1}
              {item.gstRate > 0 && <span className="gst-badge">GST {item.gstRate}%</span>}
            </div>
            <div className="row g-3 mb-2">
              <div className="col-md-3">
                <input
                  type="text"
                  className="form-control"
                  placeholder="Item Name"
                  value={item.name}
                  onChange={(e) =>
                    handleItemChange(index, "name", e.target.value)
                  }
                />
              </div>
              <div className="col-md-2">
                <input
                  type="number"
                  className="form-control"
                  placeholder="Qty"
                  value={item.qty}
                  onChange={(e) =>
                    handleItemChange(index, "qty", Number(e.target.value))
                  }
                />
              </div>
              <div className="col-md-2">
                <input
                  type="number"
                  className="form-control"
                  placeholder="Amount"
                  value={item.amount}
                  onChange={(e) =>
                    handleItemChange(index, "amount", Number(e.target.value))
                  }
                />
              </div>
              <div className="col-md-2">
                <input
                  type="number"
                  className={`form-control ${validationErrors[`item_${index}_gstRate`] ? 'is-invalid' : ''}`}
                  placeholder="GST %"
                  value={item.gstRate || 0}
                  onChange={(e) => {
                    const rate = Number(e.target.value);
                    const validation = validateGSTRate(rate);
                    
                    if (validation.isValid) {
                      handleItemChange(index, "gstRate", rate);
                      // Clear error
                      setValidationErrors(prev => {
                        // eslint-disable-next-line no-unused-vars
                        const { [`item_${index}_gstRate`]: removed, ...rest } = prev;
                        return rest;
                      });
                    } else {
                      // Set error
                      setValidationErrors(prev => ({ 
                        ...prev, 
                        [`item_${index}_gstRate`]: validation.error 
                      }));
                    }
                  }}
                  min="0"
                  max="28"
                  step="0.01"
                />
                {validationErrors[`item_${index}_gstRate`] && (
                  <div className="invalid-feedback d-block">
                    {validationErrors[`item_${index}_gstRate`]}
                  </div>
                )}
              </div>
              <div className="col-md-3">
                <input
                  type="number"
                  className="form-control bg-light"
                  placeholder="Total"
                  value={item.total}
                  readOnly
                />
              </div>
            </div>
            
            {/* GST Breakdown Row */}
            {(item.gstRate > 0) && (
              <div className="row g-3 mb-2">
                {invoiceData.transactionType === 'INTRA_STATE' ? (
                  <>
                    <div className="col-md-4">
                      <label className="form-label small text-muted">CGST</label>
                      <input
                        type="number"
                        className="form-control form-control-sm bg-light"
                        value={item.cgstAmount?.toFixed(2) || 0}
                        readOnly
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label small text-muted">SGST</label>
                      <input
                        type="number"
                        className="form-control form-control-sm bg-light"
                        value={item.sgstAmount?.toFixed(2) || 0}
                        readOnly
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label small text-muted">Total with GST</label>
                      <input
                        type="number"
                        className="form-control form-control-sm bg-light"
                        value={item.totalWithGST?.toFixed(2) || 0}
                        readOnly
                      />
                    </div>
                  </>
                ) : (
                  <>
                    <div className="col-md-6">
                      <label className="form-label small text-muted">IGST</label>
                      <input
                        type="number"
                        className="form-control form-control-sm bg-light"
                        value={item.igstAmount?.toFixed(2) || 0}
                        readOnly
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label small text-muted">Total with GST</label>
                      <input
                        type="number"
                        className="form-control form-control-sm bg-light"
                        value={item.totalWithGST?.toFixed(2) || 0}
                        readOnly
                      />
                    </div>
                  </>
                )}
              </div>
            )}
            
            <div className="d-flex gap-2">
              <textarea
                className="form-control"
                placeholder="Description"
                value={item.description}
                onChange={(e) =>
                  handleItemChange(index, "description", e.target.value)
                }
              />
              {invoiceData.items.length > 1 && (
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  onClick={() => deleteItem(index)}
                >
                  <Trash2 size={18} />
                </button>
              )}
            </div>
          </div>
        ))}
        <button type="button" className="btn btn-primary" onClick={addItem}>
          Add Item
        </button>
      </div>

      {/* Account info */}
      <div className="mb-4">
        <h5><CreditCard size={20} /> Bank Account Details</h5>
        <div className="row g-3">
          <div className="col-md-4">
            <input
              type="text"
              placeholder="Account Name"
              className="form-control"
              value={invoiceData.account.name}
              onChange={(e) => handleChange("account", "name", e.target.value)}
            />
          </div>
          <div className="col-md-4">
            <input
              type="text"
              className="form-control"
              placeholder="Account number"
              value={invoiceData.account.number}
              onChange={(e) =>
                handleChange("account", "number", e.target.value)
              }
            />
          </div>
          <div className="col-md-4">
            <input
              type="text"
              className="form-control"
              placeholder="Branch/IFSC Code"
              value={invoiceData.account.ifsccode}
              onChange={(e) =>
                handleChange("account", "ifsccode", e.target.value)
              }
            />
          </div>
        </div>
      </div>

      {/* TOTALS */}
      <div className="mb-4">
        <h5><Calculator size={20} /> Totals</h5>
        <div className="d-flex justify-content-end">
          <div className="w-100 w-md-50 totals-section">
            <div className="d-flex justify-content-between total-row">
              <span className="fw-semibold">Subtotal (before GST)</span>
              <span className="fw-semibold">₹{subtotal.toFixed(2)}</span>
            </div>
            
            {/* GST Breakdown */}
            {gstTotal > 0 && (
              <>
                {invoiceData.transactionType === 'INTRA_STATE' ? (
                  <>
                    <div className="d-flex justify-content-between total-row text-muted">
                      <span>CGST</span>
                      <span>₹{cgstTotal.toFixed(2)}</span>
                    </div>
                    <div className="d-flex justify-content-between total-row text-muted">
                      <span>SGST</span>
                      <span>₹{sgstTotal.toFixed(2)}</span>
                    </div>
                  </>
                ) : (
                  <div className="d-flex justify-content-between total-row text-muted">
                    <span>IGST</span>
                    <span>₹{igstTotal.toFixed(2)}</span>
                  </div>
                )}
                <div className="d-flex justify-content-between total-row">
                  <span className="fw-semibold">Total GST</span>
                  <span className="fw-semibold">₹{gstTotal.toFixed(2)}</span>
                </div>
              </>
            )}
            
            {/* Legacy Tax (if used) */}
            {taxAmount > 0 && (
              <>
                <div className="d-flex justify-content-between align-items-center total-row my-2">
                  <label htmlFor="taxInput" className="me-2">
                    Additional Tax Rate (%)
                  </label>
                  <input
                    id="taxInput"
                    type="number"
                    className="form-control w-50 text-end"
                    value={invoiceData.tax}
                    onChange={(e) =>
                      setInvoiceData((prev) => ({ ...prev, tax: e.target.value }))
                    }
                  />
                </div>
                <div className="d-flex justify-content-between total-row">
                  <span>Additional Tax Amount</span>
                  <span>₹{taxAmount.toFixed(2)}</span>
                </div>
              </>
            )}
            
            <div className="d-flex justify-content-between total-row grand-total">
              <span>Grand Total</span>
              <span>₹{grandTotal.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* NOTES */}
      <div className="mb-4">
        <h5>Notes:</h5>
        <div className="w-100">
          <textarea
            name="notes"
            rows="3"
            className="form-control"
            value={invoiceData.notes}
            onChange={(e) =>
              setInvoiceData((prev) => ({ ...prev, notes: e.target.value }))
            }
          ></textarea>
        </div>
      </div>
    </div>
  );
};

export default InvoiceForm;
