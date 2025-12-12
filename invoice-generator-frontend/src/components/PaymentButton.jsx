import React, { useState } from 'react';
import { DollarSign } from 'lucide-react';
import paymentService from '../service/paymentService';
import toast from 'react-hot-toast';
import { useAuth } from '@clerk/clerk-react';

const PaymentButton = ({ invoice, onPaymentSuccess, size = 'md', variant = 'primary' }) => {
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
                if (onPaymentSuccess) {
                    onPaymentSuccess();
                }
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

    const getSizeClass = () => {
        switch (size) {
            case 'sm': return 'btn-sm';
            case 'lg': return 'btn-lg';
            default: return '';
        }
    };

    const getVariantClass = () => {
        switch (variant) {
            case 'success': return 'btn-success';
            case 'outline': return 'btn-outline-primary';
            case 'outline-success': return 'btn-outline-success';
            default: return 'btn-primary';
        }
    };

    return (
        <button
            className={`btn ${getVariantClass()} ${getSizeClass()} d-flex align-items-center gap-2`}
            onClick={handleCashPayment}
            disabled={loading}
            title={`Mark as Paid - ₹${calculateTotal().toFixed(2)}`}
        >
            {loading ? (
                <div className="spinner-border spinner-border-sm" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
            ) : (
                <DollarSign size={size === 'sm' ? 14 : size === 'lg' ? 20 : 16} />
            )}
            {size !== 'sm' && (loading ? 'Marking...' : 'Mark Paid')}
        </button>
    );
};

export default PaymentButton;