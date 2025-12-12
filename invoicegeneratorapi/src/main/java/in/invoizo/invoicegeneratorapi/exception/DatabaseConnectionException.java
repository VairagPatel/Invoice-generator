package in.invoizo.invoicegeneratorapi.exception;

/**
 * Exception thrown when database connection or operations fail
 */
public class DatabaseConnectionException extends RuntimeException {
    
    private final boolean retryable;
    
    public DatabaseConnectionException(String message) {
        super(message);
        this.retryable = true;
    }
    
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }
    
    public DatabaseConnectionException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
}
