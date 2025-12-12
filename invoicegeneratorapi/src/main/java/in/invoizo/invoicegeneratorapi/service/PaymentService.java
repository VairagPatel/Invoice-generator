package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final InvoiceService invoiceService;



    public void markCashPayment(String invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        
        Invoice.PaymentDetails paymentDetails = invoice.getPaymentDetails();
        if (paymentDetails == null) {
            paymentDetails = new Invoice.PaymentDetails();
        }

        paymentDetails.setPaymentStatus(Invoice.PaymentStatus.PAID);
        paymentDetails.setPaymentMethod(Invoice.PaymentMethod.CASH);
        paymentDetails.setPaymentDate(Instant.now());

        invoice.setPaymentDetails(paymentDetails);
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());

        invoiceService.updateInvoice(invoice);
        log.info("Cash payment marked for invoice {}", invoiceId);
    }


}