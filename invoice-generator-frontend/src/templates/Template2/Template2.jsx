import React from 'react';
import './template2.css';

const Template2 = ({ data }) => {
    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            minimumFractionDigits: 2
        }).format(amount);
    };
    
    // Check if GST details are available
    const hasGST = data.hasGST || false;
    const isIntraState = data.transactionType === 'INTRA_STATE';

    return (
        <div className="template2 container border p-4 mt-4 template2-wrapper">
            {/* Header */}
            <div className="d-flex justify-content-between align-items-start">
                <h2 className="fw-bold text-success">Invoice</h2>
                <div className="text-end w-50">
                    {data.companyLogo && (
                        <div className="mb-2">
                            <img
                                src={data.companyLogo}
                                alt="Company Logo"
                                width={98}
                            />
                        </div>
                    )}
                    <h6 className="fw-bold company-title">{data?.companyName}</h6>
                    <p className="mb-0">{data?.companyAddress}</p>
                    <p className="mb-0">{data?.companyPhone}</p>
                    {data.companyGSTNumber && <p className="mb-0">GST No: {data.companyGSTNumber}</p>}
                </div>
            </div>

            {/* Client and Invoice Info */}
            <div className="row mt-4">
                <div className="col-md-6">
                    <h6 className="text-success fw-semibold">Billed To</h6>
                    <p className="mb-0 fw-bold">{data?.billingName}</p>
                    <p className="mb-0">{data?.billingAddress}</p>
                    <p className="mb-0">{data?.billingPhone}</p>
                </div>
                <div className="col-md-6 text-md-end">
                    <h6 className="text-success fw-semibold">Invoice Details</h6>
                    <p className="mb-0"><strong>Invoice #:</strong> {data?.invoiceNumber}</p>
                    <p className="mb-0"><strong>Invoice Date:</strong> {data?.invoiceDate}</p>
                    <p className="mb-0"><strong>Due Date:</strong> {data?.paymentDate}</p>
                </div>
            </div>

            {/* Items Table */}
            <div className="table-responsive mt-4">
                <table className="table table-bordered" aria-label="Invoice Items Table">
                    <thead className="bg-success text-white">
                    <tr>
                        <th className="col template2-table-header">Item Description</th>
                        <th className="col template2-table-header">Qty</th>
                        <th className="col template2-table-header">Rate</th>
                        {hasGST && <th className="col template2-table-header">GST %</th>}
                        {hasGST && isIntraState && <th className="col template2-table-header">CGST</th>}
                        {hasGST && isIntraState && <th className="col template2-table-header">SGST</th>}
                        {hasGST && !isIntraState && <th className="col template2-table-header">IGST</th>}
                        <th className="col template2-table-header">Amount</th>
                    </tr>
                    </thead>
                    <tbody>
                    {data?.items?.map((item, index) => (
                        <tr key={index} className={index % 2 !== 0 ? 'table-light' : ''}>
                            <td>{item.name}</td>
                            <td>{item.qty}</td>
                            <td>{formatCurrency(item.amount)}</td>
                            {hasGST && <td>{item.gstRate || 0}%</td>}
                            {hasGST && isIntraState && <td>{formatCurrency(item.cgstAmount || 0)}</td>}
                            {hasGST && isIntraState && <td>{formatCurrency(item.sgstAmount || 0)}</td>}
                            {hasGST && !isIntraState && <td>{formatCurrency(item.igstAmount || 0)}</td>}
                            <td>{formatCurrency(hasGST && item.totalWithGST ? item.totalWithGST : item.qty * item.amount)}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {/* Summary */}
            <div className="d-flex justify-content-end mt-3">
                <div className="text-end">
                    {data.transactionType && (
                        <p className="mb-1"><strong>Transaction Type:</strong> {data.transactionType === 'INTRA_STATE' ? 'Intra-State' : 'Inter-State'}</p>
                    )}
                    <p className="mb-1"><strong>Sub Total:</strong> {formatCurrency(data?.subtotal || 0)}</p>
                    {hasGST ? (
                        <>
                            {isIntraState ? (
                                <>
                                    <p className="mb-1"><strong>CGST:</strong> {formatCurrency(data?.cgstTotal || 0)}</p>
                                    <p className="mb-1"><strong>SGST:</strong> {formatCurrency(data?.sgstTotal || 0)}</p>
                                </>
                            ) : (
                                <p className="mb-1"><strong>IGST:</strong> {formatCurrency(data?.igstTotal || 0)}</p>
                            )}
                            <p className="mb-1"><strong>Total GST:</strong> {formatCurrency(data?.gstTotal || 0)}</p>
                        </>
                    ) : (
                        <p className="mb-1"><strong>Tax ({data?.tax || 0}%):</strong> {formatCurrency(data?.taxAmount || 0)}</p>
                    )}
                    <p className="fw-bold text-success fs-5">Total Due: {formatCurrency(data?.total || 0)}</p>
                </div>
            </div>

            {/* Bank Account Details Section */}
            {(data.accountName || data.accountNumber || data.accountIfscCode) && (
                <div className="mt-4">
                    <h6 className="mb-2 text-success fw-semibold">Bank Account Details</h6>
                    {data.accountName && <p className="mb-1"><strong>Account Holder:</strong> {data.accountName}</p>}
                    {data.accountNumber && <p className="mb-1"><strong>Account Number:</strong> {data.accountNumber}</p>}
                    {data.accountIfscCode && <p className="mb-0"><strong>IFSC / Branch Code:</strong> {data.accountIfscCode}</p>}
                </div>
            )}

            {/* Payment Options Section */}
            {(data.paymentDetails?.paymentLink || data.paymentDetails?.cashPaymentAllowed) && (
                <div className="mt-4">
                    <h6 className="mb-2 text-success fw-semibold">Payment Options</h6>
                    <div className="p-3 border rounded">
                        {data.paymentDetails?.paymentLink && (
                            <div className="mb-2">
                                <p className="mb-1"><strong>Online Payment:</strong></p>
                                <p className="mb-1 small">Pay securely using UPI, Cards, Net Banking, or Wallets</p>
                                <p className="mb-0">
                                    <strong>Payment Link:</strong>{" "}
                                    <a 
                                        href={data.paymentDetails.paymentLink} 
                                        target="_blank" 
                                        rel="noopener noreferrer"
                                        className="text-success"
                                    >
                                        Click here to pay online
                                    </a>
                                </p>
                            </div>
                        )}
                        {data.paymentDetails?.cashPaymentAllowed && (
                            <div className="mb-0">
                                <p className="mb-1"><strong>Cash Payment:</strong></p>
                                <p className="mb-0 small">You can also pay by cash. Please inform us once the payment is made.</p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Notes */}
            {data?.notes && (
                <div className="mt-4 p-3 notes-section">
                    <h6 className="text-success fw-semibold">Additional Notes</h6>
                    <p className="mb-0">{data.notes}</p>
                </div>
            )}
        </div>
    );
};

export default Template2;
