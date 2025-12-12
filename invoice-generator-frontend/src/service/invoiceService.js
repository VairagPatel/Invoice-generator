import axios from "axios";

export const getAllInvoices =  (baseURL, token) => {
    return axios.get(`${baseURL}/invoices`, {headers: {Authorization: `Bearer ${token}`}});
};

export const saveInvoice = (baseURL, payload, token) => {
    return axios.post(`${baseURL}/invoices`, payload, {headers: {Authorization: `Bearer ${token}`}});
};

export const deleteInvoice = (baseURL, id, token) => {
    return axios.delete(`${baseURL}/invoices/${id}`, {headers: {Authorization: `Bearer ${token}`}});
};

export const sendInvoice = (baseURL, token, formData) => {
    return axios.post(`${baseURL}/invoices/sendinvoice`, formData, {headers: {Authorization: `Bearer ${token}`}});
}

export const updateInvoiceStatus = (baseURL, invoiceId, status, token) => {
    return axios.patch(`${baseURL}/invoices/${invoiceId}/status`, { status }, {headers: {Authorization: `Bearer ${token}`}});
}

export const filterInvoicesByStatus = (baseURL, status, token) => {
    const params = status ? { status } : {};
    return axios.get(`${baseURL}/invoices/filter`, {
        params,
        headers: {Authorization: `Bearer ${token}`}
    });
}



export const exportInvoices = (baseURL, format, invoiceIds, token) => {
    // Filter out empty, null, or undefined values and join
    const cleanInvoiceIds = invoiceIds
        .filter(id => id && id.trim() !== '')
        .join(',');
    
    const params = { format };
    
    // Only add invoiceIds parameter if we have valid IDs
    if (cleanInvoiceIds) {
        params.invoiceIds = cleanInvoiceIds;
    }
    
    return axios.get(`${baseURL}/invoices/export`, {
        params,
        headers: { Authorization: `Bearer ${token}` },
        responseType: 'blob' // Important for binary data
    });
}

export const getOverdueInvoices = (baseURL, token) => {
    return axios.get(`${baseURL}/invoices/overdue`, {
        headers: { Authorization: `Bearer ${token}` }
    });
}
