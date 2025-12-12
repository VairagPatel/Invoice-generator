export const formatInvoiceData = (invoiceData) => {
    const {
        title,
        company = {},
        invoice = {},
        account = {},
        billing = {},
        shipping = {},
        tax = 0,
        notes = "",
        items = [],
        logo = "",
        companyGSTNumber = "",
        transactionType = "",
        gstDetails = {}
    } = invoiceData || {};

    const currencySymbol = "₹";
    
    // Calculate subtotal from items
    const subtotal = items.reduce((acc, item) => acc + (item.qty * item.amount), 0);
    
    // Calculate GST totals from items
    const cgstTotal = items.reduce((acc, item) => acc + (item.cgstAmount || 0), 0);
    const sgstTotal = items.reduce((acc, item) => acc + (item.sgstAmount || 0), 0);
    const igstTotal = items.reduce((acc, item) => acc + (item.igstAmount || 0), 0);
    const gstTotal = cgstTotal + sgstTotal + igstTotal;
    
    // Legacy tax calculation (if tax field is used)
    const taxAmount = subtotal * (tax / 100);
    
    // Total includes GST and any additional tax
    const total = subtotal + gstTotal + taxAmount;
    
    const hasGST = gstTotal > 0;

    return {
        title,
        companyName: company.name,
        companyAddress: company.address,
        companyPhone: company.phone,
        companyLogo: logo,
        companyGSTNumber,

        invoiceNumber: invoice.number,
        invoiceDate: invoice.date,
        paymentDate: invoice.dueDate,

        accountName: account.name,
        accountNumber: account.number,
        accountIfscCode: account.ifsccode,

        billingName: billing.name,
        billingAddress: billing.address,
        billingPhone: billing.phone,

        shippingName: shipping.name,
        shippingAddress: shipping.address,
        shippingPhone: shipping.phone,

        currencySymbol,
        tax,
        items,
        notes,
        subtotal,
        taxAmount,
        total,
        
        // GST fields
        transactionType,
        cgstTotal,
        sgstTotal,
        igstTotal,
        gstTotal,
        hasGST
    };
};

export const formatDate = (dateStr) => {
  if (!dateStr) return "N/A";

  const date = new Date(dateStr);
  return date.toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};