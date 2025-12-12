package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.util.ValidationUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ValidationUtil validationUtil;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    /**
     * Sends an invoice email
     * 
     * @param toEmail Recipient email address
     * @param file Invoice PDF file
     * @throws MessagingException if email sending fails
     * @throws IOException if file reading fails
     */
    public void sendInvoiceEmail(String toEmail, MultipartFile file) throws MessagingException, IOException {
        // Validate email address
        validationUtil.validateEmail(toEmail);
        
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your Invoice");
        
        String textBody = "Dear Customer,\n\n" +
                        "Please find attached your invoice.\n\n" +
                        "Thank you!";
        helper.setText(textBody);
        
        helper.addAttachment(file.getOriginalFilename(), new ByteArrayResource(file.getBytes()));

        mailSender.send(message);
    }

    /**
     * Sends a payment reminder email
     * 
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param body Email body
     * @param invoice Invoice details for attachment
     * @throws MessagingException if email sending fails
     */
    public void sendPaymentReminderEmail(String toEmail, String subject, String body, Invoice invoice) throws MessagingException {
        // Validate email address
        validationUtil.validateEmail(toEmail);
        
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body);

        mailSender.send(message);
    }

    /**
     * Sends an invoice email with payment link
     * 
     * @param toEmail Recipient email address
     * @param file Invoice PDF file
     * @param paymentLink Razorpay payment link
     * @throws MessagingException if email sending fails
     * @throws IOException if file reading fails
     */
    public void sendInvoiceWithPaymentLink(String toEmail, MultipartFile file, String paymentLink) throws MessagingException, IOException {
        // Validate email address
        validationUtil.validateEmail(toEmail);
        
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your Invoice with Payment Link");
        
        String textBody = "Dear Customer,\n\n" +
                        "Please find attached your invoice.\n\n" +
                        "You can pay online using this link: " + paymentLink + "\n\n" +
                        "Alternatively, you can pay by cash and inform us once the payment is made.\n\n" +
                        "Thank you!";
        helper.setText(textBody);
        
        helper.addAttachment(file.getOriginalFilename(), new ByteArrayResource(file.getBytes()));

        mailSender.send(message);
    }
}
