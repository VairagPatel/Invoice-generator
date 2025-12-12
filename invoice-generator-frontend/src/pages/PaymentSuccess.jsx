import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Home } from 'lucide-react';
import paymentService from '../service/paymentService';
import toast from 'react-hot-toast';

const PaymentSuccess = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [paymentStatus, setPaymentStatus] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        const verifyPayment = async () => {
            try {
                const razorpayPaymentId = searchParams.get('razorpay_payment_id');
                const razorpayOrderId = searchParams.get('razorpay_order_id');
                const razorpaySignature = searchParams.get('razorpay_signature');
                const invoiceId = searchParams.get('invoice_id');

                if (!razorpayPaymentId || !razorpayOrderId || !razorpaySignature) {
                    // Check if it's a successful payment link callback
                    const paymentLinkId = searchParams.get('razorpay_payment_link_id');
                    const paymentLinkStatus = searchParams.get('razorpay_payment_link_status');
                    
                    if (paymentLinkId && paymentLinkStatus === 'paid') {
                        setPaymentStatus('success');
                        toast.success('Payment completed successfully!');
                        setLoading(false);
                        return;
                    }
                    
                    setError('Invalid payment parameters');
                    setLoading(false);
                    return;
                }

                const paymentData = {
                    razorpay_payment_id: razorpayPaymentId,
                    razorpay_order_id: razorpayOrderId,
                    razorpay_signature: razorpaySignature,
                    invoice_id: invoiceId
                };

                const response = await paymentService.verifyPayment(paymentData);
                
                if (response.success) {
                    setPaymentStatus('success');
                    toast.success('Payment verified successfully!');
                } else {
                    setPaymentStatus('failed');
                    setError(response.message || 'Payment verification failed');
                    toast.error('Payment verification failed');
                }
            } catch (error) {
                console.error('Error verifying payment:', error);
                setPaymentStatus('failed');
                setError('Payment verification failed');
                toast.error('Payment verification failed');
            } finally {
                setLoading(false);
            }
        };

        verifyPayment();
    }, [searchParams]);

    const handleGoHome = () => {
        navigate('/dashboard');
    };

    if (loading) {
        return (
            <div className="container d-flex justify-content-center align-items-center" style={{ minHeight: '100vh' }}>
                <div className="card text-center p-4">
                    <div className="spinner-border text-primary mx-auto mb-3" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <h5>Verifying Payment...</h5>
                    <p className="text-muted">Please wait while we confirm your payment.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container d-flex justify-content-center align-items-center" style={{ minHeight: '100vh' }}>
            <div className="card text-center p-4" style={{ maxWidth: '500px', width: '100%' }}>
                {paymentStatus === 'success' ? (
                    <>
                        <CheckCircle size={64} className="text-success mx-auto mb-3" />
                        <h3 className="text-success mb-3">Payment Successful!</h3>
                        <p className="text-muted mb-4">
                            Your payment has been processed successfully. The invoice has been marked as paid.
                        </p>
                        <div className="alert alert-success">
                            <strong>What's next?</strong><br />
                            You will receive a payment confirmation email shortly.
                        </div>
                    </>
                ) : (
                    <>
                        <XCircle size={64} className="text-danger mx-auto mb-3" />
                        <h3 className="text-danger mb-3">Payment Failed</h3>
                        <p className="text-muted mb-4">
                            {error || 'There was an issue processing your payment. Please try again.'}
                        </p>
                        <div className="alert alert-danger">
                            <strong>Need help?</strong><br />
                            If you believe this is an error, please contact support with your payment details.
                        </div>
                    </>
                )}
                
                <div className="d-flex gap-2 justify-content-center">
                    <button className="btn btn-primary" onClick={handleGoHome}>
                        <Home size={16} className="me-2" />
                        Go to Dashboard
                    </button>
                    {paymentStatus === 'failed' && (
                        <button className="btn btn-outline-primary" onClick={() => window.history.back()}>
                            Try Again
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PaymentSuccess;