package in.invoizo.invoicegeneratorapi.exception;

/**
 * Exception thrown when webhook signature verification fails
 */
public class WebhookVerificationException extends RuntimeException {
    
    public WebhookVerificationException(String message) {
        super(message);
    }
    
    public WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
