package in.invoizo.invoicegeneratorapi.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Utility class for implementing retry logic for transient failures
 */
@Slf4j
public class RetryUtil {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_DELAY_MS = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Execute an operation with retry logic using exponential backoff
     * 
     * @param operation The operation to execute
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelayMs Initial delay between retries in milliseconds
     * @param operationName Name of the operation for logging
     * @param <T> Return type of the operation
     * @return Result of the operation
     * @throws Exception if all retry attempts fail
     */
    public static <T> T executeWithRetry(
            Supplier<T> operation,
            int maxAttempts,
            long initialDelayMs,
            String operationName) throws Exception {
        
        Exception lastException = null;
        long delayMs = initialDelayMs;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Executing {} - Attempt {}/{}", operationName, attempt, maxAttempts);
                return operation.get();
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts && isRetryable(e)) {
                    log.warn("Attempt {}/{} failed for {}: {}. Retrying in {}ms...", 
                        attempt, maxAttempts, operationName, e.getMessage(), delayMs);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    
                    // Exponential backoff
                    delayMs = (long) (delayMs * BACKOFF_MULTIPLIER);
                    
                } else {
                    log.error("Attempt {}/{} failed for {}: {}. {}",
                        attempt, maxAttempts, operationName, e.getMessage(),
                        attempt < maxAttempts ? "Not retryable" : "Max attempts reached");
                    break;
                }
            }
        }
        
        throw lastException;
    }

    /**
     * Execute an operation with default retry settings
     * 
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @param <T> Return type of the operation
     * @return Result of the operation
     * @throws Exception if all retry attempts fail
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) throws Exception {
        return executeWithRetry(operation, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS, operationName);
    }

    /**
     * Determine if an exception is retryable
     * 
     * @param exception The exception to check
     * @return true if the exception indicates a transient failure
     */
    private static boolean isRetryable(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Network and connection errors are retryable
        if (lowerMessage.contains("timeout") ||
            lowerMessage.contains("connection") ||
            lowerMessage.contains("network") ||
            lowerMessage.contains("temporarily unavailable") ||
            lowerMessage.contains("503") ||
            lowerMessage.contains("502") ||
            lowerMessage.contains("504")) {
            return true;
        }
        
        // Database connection errors are retryable
        if (lowerMessage.contains("database") && 
            (lowerMessage.contains("connection") || lowerMessage.contains("unavailable"))) {
            return true;
        }
        
        // Check exception type
        if (exception instanceof java.net.SocketTimeoutException ||
            exception instanceof java.net.ConnectException ||
            exception instanceof java.io.IOException) {
            return true;
        }
        
        return false;
    }

    /**
     * Execute a void operation with retry logic
     * 
     * @param operation The operation to execute
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelayMs Initial delay between retries in milliseconds
     * @param operationName Name of the operation for logging
     * @throws Exception if all retry attempts fail
     */
    public static void executeVoidWithRetry(
            Runnable operation,
            int maxAttempts,
            long initialDelayMs,
            String operationName) throws Exception {
        
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxAttempts, initialDelayMs, operationName);
    }

    /**
     * Execute a void operation with default retry settings
     * 
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @throws Exception if all retry attempts fail
     */
    public static void executeVoidWithRetry(Runnable operation, String operationName) throws Exception {
        executeVoidWithRetry(operation, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS, operationName);
    }
}
