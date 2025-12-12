import React from 'react';
import './Template5.css';

const Template5 = ({ data }) => {
    const subtotal = data.items.reduce((acc, item) => acc + item.qty * item.amount, 0);
    const taxAmount = (subtotal * parseFloat(data.tax || 0)) / 100;
    const total = subtotal + taxAmount;
    
    // Check if GST details are available
    const hasGST = data.hasGST || false;
    const isIntraState = data.transactionType === 'INTRA_STATE';

    const formatDate = (dateString) => {
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
        } catch (error) {
            return dateString;
        }
    };

    return (
        <div className="template5 mx-auto my-4 p-4 border rounded">
            {/* Header */}
            <div className="d-flex flex-column flex-md-row justify-content-between mb-4">
                <div>
                    {data.companyLogo && (
                        <div className="mb-2">
                            <img
                                src={data.companyLogo}
                                alt="Company Logo"
                                width={98}
                            />
                        </div>
                    )}
                    <h4 className="fw-bold">{data.companyName}</h4>
                    <p className="mb-1">{data.companyAddress}</p>
                    <p className="mb-0">{data.companyPhone}</p>
                    {data.companyGSTNumber && <p className="mb-0">GST No: {data.companyGSTNumber}</p>}
                </div>
                <div className="text-md-end w-50">
                    <h5 className="fw-bold">INVOICE</h5>
                    <p className="mb-1"><strong>Invoice No:</strong> {data.invoiceNumber}</p>
                    <p className="mb-1"><strong>Invoice Date:</strong> {formatDate(data.invoiceDate)}</p>
                    <p className="mb-0"><strong>Due Date:</strong> {formatDate(data.paymentDate)}</p>
                </div>
            </div>

            {/* Address Section */}
            <div className="row mb-4">
                <div className="col-md-6">
                    <h6 className="fw-bold">Bill To:</h6>
                    <p className="mb-1">{data.billingName}</p>
                    <p className="mb-1">{data.billingAddress}</p>
                    <p className="mb-0">{data.billingPhone}</p>
                </div>
                {data.shippingName && data.shippingAddress && data.shippingPhone && <div className="col-md-6 text-md-end">
                    <h6 className="fw-bold">Shipped To:</h6>
                    <p className="mb-1">{data.shippingName}</p>
                    <p className="mb-1">{data.shippingAddress}</p>
                    <p className="mb-0">{data.shippingPhone}</p>
                </div>}
            </div>

            {/* Items Table */}
            <div className="table-responsive mb-4">
                <table className="table mb-0 ">
                    <thead className="template5-table-head text-white table-light">
                    <tr>
                        <th className="p-3">Item # / Description</th>
                        <th className="p-3 text-center">Quantity</th>
                        <th className="p-3 text-center">Rate</th>
                        {hasGST && <th className="p-3 text-center">GST %</th>}
                        {hasGST && isIntraState && <th className="p-3 text-end">CGST</th>}
                        {hasGST && isIntraState && <th className="p-3 text-end">SGST</th>}
                        {hasGST && !isIntraState && <th className="p-3 text-end">IGST</th>}
                        <th className="p-3 text-end">Amount</th>
                    </tr>
                    </thead>
                    <tbody>
                    {data.items.map((item, index) => (
                        <tr key={index} className="items-row">
                            <td className="p-3">
                                <div className="fw-bold">{item.name}</div>
                                <div className="text-muted small">{item.description}</div>
                            </td>
                            <td className="p-3 text-center">{item.qty}</td>
                            <td className="p-3 text-center">{data.currencySymbol}{item.amount.toFixed(2)}</td>
                            {hasGST && <td className="p-3 text-center">{item.gstRate || 0}%</td>}
                            {hasGST && isIntraState && <td className="p-3 text-end">{data.currencySymbol}{(item.cgstAmount || 0).toFixed(2)}</td>}
                            {hasGST && isIntraState && <td className="p-3 text-end">{data.currencySymbol}{(item.sgstAmount || 0).toFixed(2)}</td>}
                            {hasGST && !isIntraState && <td className="p-3 text-end">{data.currencySymbol}{(item.igstAmount || 0).toFixed(2)}</td>}
                            <td className="p-3 text-end">{data.currencySymbol}{(hasGST && item.totalWithGST ? item.totalWithGST : item.qty * item.amount).toFixed(2)}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {/* Totals */}
            <div className="d-flex justify-content-end">
                <div style={{ width: '100%', maxWidth: '400px' }}>
                    <table className="table mb-0">
                        <tbody>
                        {data.transactionType && (
                            <tr>
                                <td><strong>Transaction Type</strong></td>
                                <td className="text-end">{data.transactionType === 'INTRA_STATE' ? 'Intra-State' : 'Inter-State'}</td>
                            </tr>
                        )}
                        <tr>
                            <td><strong>Subtotal</strong></td>
                            <td className="text-end">{data.currencySymbol}{data.subtotal.toFixed(2)}</td>
                        </tr>
                        {hasGST ? (
                            <>
                                {isIntraState ? (
                                    <>
                                        <tr>
                                            <td><strong>CGST</strong></td>
                                            <td className="text-end">{data.currencySymbol}{data.cgstTotal.toFixed(2)}</td>
                                        </tr>
                                        <tr>
                                            <td><strong>SGST</strong></td>
                                            <td className="text-end">{data.currencySymbol}{data.sgstTotal.toFixed(2)}</td>
                                        </tr>
                                    </>
                                ) : (
                                    <tr>
                                        <td><strong>IGST</strong></td>
                                        <td className="text-end">{data.currencySymbol}{data.igstTotal.toFixed(2)}</td>
                                    </tr>
                                )}
                                <tr>
                                    <td><strong>Total GST</strong></td>
                                    <td className="text-end">{data.currencySymbol}{data.gstTotal.toFixed(2)}</td>
                                </tr>
                            </>
                        ) : (
                            <tr>
                                <td><strong>Tax ({data.tax}%)</strong></td>
                                <td className="text-end">{data.currencySymbol}{data.taxAmount.toFixed(2)}</td>
                            </tr>
                        )}
                        <tr>
                            <td><strong>Total</strong></td>
                            <td className="text-end fw-bold">{data.currencySymbol}{data.total.toFixed(2)}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Bank Account Details Section */}
            {(data.accountName || data.accountNumber || data.accountIfscCode) && (
                <div className="mt-4">
                    <h6 className="mb-2 fw-semibold">Bank Account Details</h6>
                    {data.accountName && <p className="mb-1"><strong>Account Holder:</strong> {data.accountName}</p>}
                    {data.accountNumber && <p className="mb-1"><strong>Account Number:</strong> {data.accountNumber}</p>}
                    {data.accountIfscCode && <p className="mb-0"><strong>IFSC / Branch Code:</strong> {data.accountIfscCode}</p>}
                </div>
            )}

            {/* Payment Options Section */}
            {(data.paymentDetails?.paymentLink || data.paymentDetails?.cashPaymentAllowed) && (
                <div className="mt-4">
                    <h6 className="mb-2 fw-semibold">Payment Options</h6>
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
                                        className="text-primary"
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
                <div className="mt-5">
                    <h6 className="fw-bold">Notes</h6>
                    <p className="mb-0">{data.notes}</p>
                </div>
            )}
        </div>
    );
};

export default Template5;
