import { createContext, useState } from "react";

export const AppContext = createContext();

export const initialInvoiceData = {
  title: "Create Invoice",
  billing: { name: "", phone: "", address: "" },
  shipping: { name: "", phone: "", address: "" },
  invoice: { number: "", date: "", dueDate: "" },
  account: { name: "", number: "", ifsccode: "" },
  company: { name: "", phone: "", address: "" },
  tax: 0,
  notes: "",
  items: [{ 
    name: "", 
    qty: "", 
    amount: "", 
    description: "", 
    total: 0,
    gstRate: 0,
    cgstAmount: 0,
    sgstAmount: 0,
    igstAmount: 0,
    totalWithGST: 0
  }],
  logo: "",
  companyGSTNumber: "",
  transactionType: "INTRA_STATE",
};

export const AppContextProvider = (props) => {
  const [invoiceData, setInvoiceData] = useState(initialInvoiceData);
  const [invoiceTitle, setInvoiceTitle] = useState("Create Invoice");
  const [selectedTemplate, setSelectedTemplate] = useState("template1");
  
  // Status filter state for filtering invoices by status
  const [statusFilter, setStatusFilter] = useState(null);
  
  // Selected invoices state for export functionality
  const [selectedInvoices, setSelectedInvoices] = useState([]);
  
  // Payment link state for storing payment link information
  const [paymentLink, setPaymentLink] = useState(null);
  
  // GST configuration state
  const [gstConfig, setGstConfig] = useState({
    companyGSTNumber: "",
    transactionType: "INTRA_STATE"
  });

  const baseURL = "http://localhost:8080/api";

  const contextValue = {
    baseURL,
    invoiceData,
    setInvoiceData,
    invoiceTitle,
    setInvoiceTitle,
    selectedTemplate,
    setSelectedTemplate,
    initialInvoiceData,
    statusFilter,
    setStatusFilter,
    selectedInvoices,
    setSelectedInvoices,
    paymentLink,
    setPaymentLink,
    gstConfig,
    setGstConfig,
  };

  return (
    <AppContext.Provider value={contextValue}>
      {props.children}
    </AppContext.Provider>
  );
};
