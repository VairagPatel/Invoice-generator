import React, { useState } from 'react';
import { useAuth } from '@clerk/clerk-react';
import axios from 'axios';
import toast from 'react-hot-toast';

const TestPayment = () => {
    const [loading, setLoading] = useState(false);
    const [paymentLink, setPaymentLink] = useState('');
    const { getToken } = useAuth();

    const createTestPaymentLink = async () => {
        setLoading(true);
        try {
            const token = await getToken();
            const response = await axios.get('http://localhost:8080/api/payments/test-razorpay', {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.data.success) {
                setPaymentLink(response.data.paymentLink);
                toast.success('Test payment link created successfully!');
            } else {
                toast.error(response.data.message || 'Failed to create test payment link');
            }
        } catch (error) {
            console.error('Error creating test payment link:', error);
            toast.error('Failed to create test payment link: ' + (error.response?.data?.message || error.message));
        } finally {
            setLoading(false);
        }
    };

    const openPaymentLink = () => {
        if (paymentLink) {
            window.open(paymentLink, '_blank');
        }
    };

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-6">
                    <div className="card">
                        <div className="card-header">
                            <h5 className="mb-0">Test Razorpay Payment Link</h5>
                        </div>
                        <div className="card-body">
                            <p className="text-muted">
                                This will create a test payment link for ₹100 to verify Razorpay integration.
                            </p>
                            
                            <button 
                                className="btn btn-primary mb-3"
                                onClick={createTestPaymentLink}
                                disabled={loading}
                            >
                                {loading && (
                                    <div className="spinner-border spinner-border-sm me-2" role="status">
                                        <span className="visually-hidden">Loading...</span>
                                    </div>
                                )}
                                Create Test Payment Link
                            </button>

                            {paymentLink && (
                                <div className="alert alert-success">
                                    <h6>Payment Link Created!</h6>
                                    <p className="mb-2">
                                        <strong>Link:</strong> 
                                        <a href={paymentLink} target="_blank" rel="noopener noreferrer" className="ms-2">
                                            {paymentLink}
                                        </a>
                                    </p>
                                    <button 
                                        className="btn btn-success btn-sm"
                                        onClick={openPaymentLink}
                                    >
                                        Open Payment Link
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TestPayment;