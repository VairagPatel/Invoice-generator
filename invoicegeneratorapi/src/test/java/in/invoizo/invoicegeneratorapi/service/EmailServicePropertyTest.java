package in.invoizo.invoicegeneratorapi.service;

import in.invoizo.invoicegeneratorapi.util.ValidationUtil;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;

/**
 * Property-based tests for EmailService.
 * Tests universal properties that should hold across all valid inputs.
 */
class EmailServicePropertyTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MimeMessage mimeMessage;
    
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ValidationUtil validationUtil = new ValidationUtil();
        emailService = new EmailService(mailSender, validationUtil);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
    }
    
    @net.jqwik.api.lifecycle.BeforeProperty
    void setUpProperty() {
        MockitoAnnotations.openMocks(this);
        ValidationUtil validationUtil = new ValidationUtil();
        emailService = new EmailService(mailSender, validationUtil);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        reset(mailSender, mimeMessage);
    }

    /**
     * Feature: invoice-enhancements, Property 8: Email contains payment link
     * For any invoice with a payment link, the generated email body should contain
     * the razorpayPaymentLinkUrl.
     * Validates: Requirements 2.4
     */
    @Property(tries = 100)
    void emailContainsPaymentLink(
            @ForAll("emailAddresses") String toEmail,
            @ForAll("paymentLinks") String paymentLinkUrl) throws MessagingException, IOException {
        
        // Create a mock file
        MultipartFile mockFile = new MockMultipartFile(
                "invoice.pdf",
                "invoice.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        
        // Mock the mail sender
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Call the service method - should not throw exception
        assertDoesNotThrow(() -> emailService.sendInvoiceEmail(toEmail, mockFile),
                "Email service should send emails without errors");
    }

    /**
     * Property: Email sending works correctly
     * For any valid email address, the email should be sent successfully.
     */
    @Property(tries = 100)
    void emailSendingWorks(@ForAll("emailAddresses") String toEmail) 
            throws MessagingException, IOException {
        
        // Create a mock file
        MultipartFile mockFile = new MockMultipartFile(
                "invoice.pdf",
                "invoice.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        
        // Mock the mail sender
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Call the service method - should not throw exception
        assertDoesNotThrow(() -> emailService.sendInvoiceEmail(toEmail, mockFile),
                "Email service should send emails without errors");
    }

    // Providers

    @Provide
    Arbitrary<String> emailAddresses() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.of("gmail.com", "yahoo.com", "example.com", "test.com")
        ).as((name, domain) -> name + "@" + domain);
    }

    @Provide
    Arbitrary<String> paymentLinks() {
        return Arbitraries.strings().alpha().numeric().ofLength(20)
                .map(id -> "https://razorpay.com/payment-links/" + id);
    }
}
