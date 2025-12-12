package in.invoizo.invoicegeneratorapi.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for input validation across the application.
 * Provides validation methods for common data types and business rules.
 */
@Component
public class ValidationUtil {

    // Email validation pattern (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // GST number validation pattern (Indian GST format: 2 digits + 10 alphanumeric + 1 digit + 1 letter + 1 alphanumeric)
    private static final Pattern GST_NUMBER_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"
    );

    /**
     * Validates an email address format.
     * 
     * @param email The email address to validate
     * @throws IllegalArgumentException if email is invalid
     */
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required");
        }
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email address format");
        }
    }

    /**
     * Validates a GST number format (Indian GST format).
     * 
     * @param gstNumber The GST number to validate
     * @throws IllegalArgumentException if GST number is invalid
     */
    public void validateGSTNumber(String gstNumber) {
        if (gstNumber == null || gstNumber.trim().isEmpty()) {
            // GST number is optional, so empty is valid
            return;
        }
        
        if (!GST_NUMBER_PATTERN.matcher(gstNumber.trim()).matches()) {
            throw new IllegalArgumentException("Invalid GST number format. Expected format: 22AAAAA0000A1Z5");
        }
    }

    /**
     * Validates an export format parameter.
     * 
     * @param format The export format to validate
     * @throws IllegalArgumentException if format is invalid
     */
    public void validateExportFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid export format. Supported formats are: excel, csv");
        }
        
        String normalizedFormat = format.trim().toLowerCase();
        if (!normalizedFormat.equals("excel") && !normalizedFormat.equals("csv")) {
            throw new IllegalArgumentException("Invalid export format. Supported formats are: excel, csv");
        }
    }

    /**
     * Validates that a user is authorized to access a resource.
     * 
     * @param resourceOwnerId The clerk ID of the resource owner
     * @param requestingUserId The clerk ID of the user making the request
     * @throws IllegalArgumentException if user is not authorized
     */
    public void validateAuthorization(String resourceOwnerId, String requestingUserId) {
        if (requestingUserId == null || requestingUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Authentication required");
        }
        
        if (resourceOwnerId == null || !resourceOwnerId.equals(requestingUserId)) {
            throw new IllegalArgumentException("Access denied");
        }
    }

    /**
     * Validates that a string is not null or empty.
     * 
     * @param value The string to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if value is null or empty
     */
    public void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    /**
     * Validates that a value is within a specified range.
     * 
     * @param value The value to validate
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if value is outside the range
     */
    public void validateRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %.2f and %.2f", fieldName, min, max)
            );
        }
    }
}
