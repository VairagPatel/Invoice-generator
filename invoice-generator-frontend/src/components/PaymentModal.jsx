import React, { useState } from 'react';
import { DollarSign, X } from 'lucide-react';
import paymentService from '../service/paymentService';
import toast from 'react-hot-toast';
import { useAuth } from '@clerk/clerk-react';

const PaymentModal = ({ show, onHide, invoice, onPaymentSuccess }) => {
    const [loading, setLoading] = useState(false);
    const { getToken } = useAuth();

    const handleCashPayment = async () => {
        setLoading(true);
        try {
            const invoiceId = invoice.id || invoice._id;
            if (!invoiceId) {
                toast.error('Invoice ID not found');
                return;
            }
            const token = await getToken();
            const response = await paymentService.markCashPayment(invoiceId, token);
            if (response.success) {
                toast.success('Cash payment marked successfully!');
                onPaymentSuccess();
                onHide();
            } else {
                toast.error(response.message || 'Failed to mark cash payment');
            }
        } catch (error) {
            console.error('Error marking cash payment:', error);
            toast.error('Failed to mark cash payment');
        } finally {
            setLoading(false);
        }
    };

    const calculateTotal = () => {
        if (!invoice) return 0;
        
        const subtotal = invoice.items?.reduce((sum, item) => 
            sum + (item.qty * item.amount), 0) || 0;
        
        const gstTotal = invoice.gstDetails?.gstTotal || 0;
        const tax = invoice.tax || 0;
        
        return subtotal + gstTotal + tax;
    };

    if (!show) return null;

    return (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">Cash Payment</h5>
                        <button type="button" className="btn-close" onClick={onHide}>
                            <X size={20} />
                        </button>
                    </div>
                    <div className="modal-body">
                        {invoice && (
                            <div>
                                <div className="mb-4 p-3 bg-light rounded">
                                    <h6>Invoice Details</h6>
                                    <p className="mb-1"><strong>Invoice #:</strong> {invoice.invoice?.number}</p>
                                    <p className="mb-1"><strong>Amount:</strong> ₹{calculateTotal().toFixed(2)}</p>
                                    <p className="mb-0"><strong>Due Date:</strong> {invoice.invoice?.dueDate}</p>
                                </div>

                                <div className="mb-3">
                                    <div className="alert alert-warning">
                                        <strong>Cash Payment</strong><br />
                                        Mark this invoice as paid if you have received cash payment from the customer.
                                        <br /><small>This action will update the invoice status to "PAID".</small>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                    <div className="modal-footer">
                        <button className="btn btn-secondary" onClick={onHide}>
                            Cancel
                        </button>
                        
                        <button
                            className="btn btn-success d-flex align-items-center gap-2"
                            onClick={handleCashPayment}
                            disabled={loading}
                        >
                            {loading ? (
                                <div className="spinner-border spinner-border-sm" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                            ) : (
                                <DollarSign size={16} />
                            )}
                            {loading ? 'Marking...' : 'Mark as Paid (Cash)'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PaymentModal;