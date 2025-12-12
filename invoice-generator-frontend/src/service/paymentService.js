import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

class PaymentService {
    constructor() {
        this.api = axios.create({
            baseURL: API_BASE_URL,
            headers: {
                'Content-Type': 'application/json',
            },
        });
    }

    async markCashPayment(invoiceId, token = null) {
        try {
            const headers = {};
            if (token) {
                headers.Authorization = `Bearer ${token}`;
            }
            const response = await this.api.post(`/payments/mark-cash-payment/${invoiceId}`, {}, { headers });
            return response.data;
        } catch (error) {
            console.error('Error marking cash payment:', error);
            throw error;
        }
    }
}

export default new PaymentService();