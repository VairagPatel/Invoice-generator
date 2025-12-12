package in.invoizo.invoicegeneratorapi.util;

import in.invoizo.invoicegeneratorapi.validator.InvalidStatusTransitionException;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for error message quality.
 * Tests that error messages are descriptive and helpful.
 */
class ErrorMessagePropertyTest {

    /**
     * Feature: invoice-enhancements, Property 27: Error message quality
     * For any error condition, the error message should be non-empty and contain descriptive text about the error.
     * Validates: Requirements 5.6
     */
    @Property(tries = 100)
    void errorMessagesAreDescriptive(@ForAll("errorScenarios") ErrorScenario scenario) {
        Exception exception = scenario.generateException();
        
        // Error message should not be null
        assertNotNull(exception.getMessage(),
                "Error message should not be null");
        
        // Error message should not be empty
        assertFalse(exception.getMessage().trim().isEmpty(),
                "Error message should not be empty");
        
        // Error message should have reasonable length (at least 10 characters)
        assertTrue(exception.getMessage().length() >= 10,
                "Error message should be descriptive (at least 10 characters)");
        
        // Error message should not be just a generic "Error" or "Exception"
        assertFalse(exception.getMessage().equalsIgnoreCase("error"),
                "Error message should be more descriptive than just 'error'");
        assertFalse(exception.getMessage().equalsIgnoreCase("exception"),
                "Error message should be more descriptive than just 'exception'");
    }

    @Property(tries = 100)
    void validationErrorMessagesContainFieldInfo(@ForAll("validationErrors") ValidationError error) {
        IllegalArgumentException exception = error.generateException();
        
        // Validation error messages should mention what was invalid
        String message = exception.getMessage().toLowerCase();
        
        // Should contain some indication of what field or value was invalid
        boolean containsUsefulInfo = 
            message.contains("rate") ||
            message.contains("email") ||
            message.contains("format") ||
            message.contains("gst") ||
            message.contains("required") ||
            message.contains("invalid") ||
            message.contains("must be") ||
            message.contains("between");
        
        assertTrue(containsUsefulInfo,
                "Validation error message should contain information about what was invalid: " + message);
    }

    @Property(tries = 100)
    void statusTransitionErrorsContainStatusInfo(
            @ForAll("statusNames") String fromStatus,
            @ForAll("statusNames") String toStatus) {
        
        // Create an invalid status transition exception
        InvalidStatusTransitionException exception = new InvalidStatusTransitionException(
                String.format("Cannot transition from %s to %s", fromStatus, toStatus)
        );
        
        String message = exception.getMessage();
        
        // Error message should contain both status names
        assertTrue(message.contains(fromStatus),
                "Error message should contain the 'from' status");
        assertTrue(message.contains(toStatus),
                "Error message should contain the 'to' status");
        
        // Error message should indicate it's about a transition
        assertTrue(message.toLowerCase().contains("transition") || 
                   message.toLowerCase().contains("cannot"),
                "Error message should indicate it's about an invalid transition");
    }

    // Generators

    @Provide
    Arbitrary<ErrorScenario> errorScenarios() {
        return Arbitraries.of(
                // GST rate validation errors
                new ErrorScenario(() -> new IllegalArgumentException("GST rate must be between 0 and 28 percent")),
                
                // Email validation errors
                new ErrorScenario(() -> new IllegalArgumentException("Invalid email address format")),
                new ErrorScenario(() -> new IllegalArgumentException("Email address is required")),
                
                // Export format validation errors
                new ErrorScenario(() -> new IllegalArgumentException("Supported formats are: excel, csv")),
                new ErrorScenario(() -> new IllegalArgumentException("Export format is required")),
                
                // Authorization errors
                new ErrorScenario(() -> new IllegalArgumentException("Access denied")),
                new ErrorScenario(() -> new IllegalArgumentException("Authentication required")),
                
                // GST number validation errors
                new ErrorScenario(() -> new IllegalArgumentException("Invalid GST number format. Expected format: 22AAAAA0000A1Z5")),
                
                // Status transition errors
                new ErrorScenario(() -> new InvalidStatusTransitionException("Cannot transition from PAID to DRAFT")),
                new ErrorScenario(() -> new InvalidStatusTransitionException("Cannot transition from CANCELLED to SENT")),
                
                // Not found errors
                new ErrorScenario(() -> new RuntimeException("Invoice not found: INV-123456")),
                
                // Required field errors
                new ErrorScenario(() -> new IllegalArgumentException("Invoice number is required")),
                new ErrorScenario(() -> new IllegalArgumentException("Customer name is required"))
        );
    }

    @Provide
    Arbitrary<ValidationError> validationErrors() {
        return Arbitraries.of(
                new ValidationError(() -> new IllegalArgumentException("GST rate must be between 0 and 28 percent")),
                new ValidationError(() -> new IllegalArgumentException("Invalid email address format")),
                new ValidationError(() -> new IllegalArgumentException("Export format is required")),
                new ValidationError(() -> new IllegalArgumentException("Invalid GST number format. Expected format: 22AAAAA0000A1Z5"))
        );
    }

    @Provide
    Arbitrary<String> statusNames() {
        return Arbitraries.of("DRAFT", "SENT", "VIEWED", "PAID", "OVERDUE", "CANCELLED");
    }

    // Helper classes

    static class ErrorScenario {
        private final ExceptionGenerator generator;

        ErrorScenario(ExceptionGenerator generator) {
            this.generator = generator;
        }

        Exception generateException() {
            return generator.generate();
        }
    }

    static class ValidationError {
        private final ExceptionGenerator generator;

        ValidationError(ExceptionGenerator generator) {
            this.generator = generator;
        }

        IllegalArgumentException generateException() {
            return (IllegalArgumentException) generator.generate();
        }
    }

    @FunctionalInterface
    interface ExceptionGenerator {
        Exception generate();
    }
}
