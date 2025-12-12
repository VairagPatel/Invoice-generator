package in.invoizo.invoicegeneratorapi.exception;

/**
 * Exception thrown when export file generation fails
 */
public class ExportGenerationException extends RuntimeException {
    
    public ExportGenerationException(String message) {
        super(message);
    }
    
    public ExportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
