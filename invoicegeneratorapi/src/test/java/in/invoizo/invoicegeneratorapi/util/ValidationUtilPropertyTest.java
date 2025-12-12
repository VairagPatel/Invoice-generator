package in.invoizo.invoicegeneratorapi.util;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ValidationUtil.
 * Tests input validation consistency and authorization checks.
 */
class ValidationUtilPropertyTest {

    private final ValidationUtil validationUtil = new ValidationUtil();

    /**
     * Feature: invoice-enhancements, Property 28: Input validation consistency
     * For any invalid input, both frontend validation and backend validation should reject it.
     * Validates: Requirements 5.7
     * 
     * This test verifies that backend validation consistently rejects invalid inputs.
     */
    @Property(tries = 100)
    void invalidEmailsAreRejected(@ForAll("invalidEmails") String invalidEmail) {
        // When validating an invalid email
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationUtil.validateEmail(invalidEmail)
        );

        // Then the exception message should be descriptive
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("email") || 
                   exception.getMessage().contains("Email"),
                "Error message should mention email");
    }

    @Property(tries = 100)
    void validEmailsAreAccepted(@ForAll("validEmails") String validEmail) {
        // When validating a valid email
        // Then no exception should be thrown
        assertDoesNotThrow(() -> validationUtil.validateEmail(validEmail),
                "Valid emails should be accepted");
    }

    @Property(tries = 100)
    void invalidExportFormatsAreRejected(@ForAll("invalidFormats") String invalidFormat) {
        // When validating an invalid export format
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationUtil.validateExportFormat(invalidFormat)
        );

        // Then the exception message should be descriptive
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("format"),
                "Error message should mention format");
    }

    @Property(tries = 100)
    void validExportFormatsAreAccepted(@ForAll("validFormats") String validFormat) {
        // When validating a valid export format
        // Then no exception should be thrown
        assertDoesNotThrow(() -> validationUtil.validateExportFormat(validFormat),
                "Valid export formats should be accepted");
    }

    /**
     * Feature: invoice-enhancements, Property 29: Data access authorization
     * For any database query for invoices, the results should only include invoices
     * where clerkId equals the authenticated user's ID.
     * Validates: Requirements 5.8
     * 
     * This test verifies that authorization validation correctly identifies unauthorized access.
     */
    @Property(tries = 100)
    void unauthorizedAccessIsRejected(
            @ForAll("userIds") String resourceOwnerId,
            @ForAll("userIds") String requestingUserId) {
        
        Assume.that(!resourceOwnerId.equals(requestingUserId));
        
        // When validating authorization for different users
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationUtil.validateAuthorization(resourceOwnerId, requestingUserId)
        );

        // Then the exception message should indicate access denial
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("access") ||
                   exception.getMessage().toLowerCase().contains("denied"),
                "Error message should indicate access denial");
    }

    @Property(tries = 100)
    void authorizedAccessIsAccepted(@ForAll("userIds") String userId) {
        // When validating authorization for the same user
        // Then no exception should be thrown
        assertDoesNotThrow(() -> validationUtil.validateAuthorization(userId, userId),
                "User should be authorized to access their own resources");
    }

    @Property(tries = 100)
    void nullAuthenticationIsRejected(@ForAll("userIds") String resourceOwnerId) {
        // When validating authorization with null requesting user
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationUtil.validateAuthorization(resourceOwnerId, null)
        );

        // Then the exception message should indicate authentication is required
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("authentication") ||
                   exception.getMessage().toLowerCase().contains("required"),
                "Error message should indicate authentication is required");
    }

    @Property(tries = 100)
    void invalidGSTNumbersAreRejected(@ForAll("invalidGSTNumbers") String invalidGSTNumber) {
        // When validating an invalid GST number
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationUtil.validateGSTNumber(invalidGSTNumber)
        );

        // Then the exception message should be descriptive
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toUpperCase().contains("GST"),
                "Error message should mention GST");
    }

    @Property(tries = 100)
    void validGSTNumbersAreAccepted(@ForAll("validGSTNumbers") String validGSTNumber) {
        // When validating a valid GST number
        // Then no exception should be thrown
        assertDoesNotThrow(() -> validationUtil.validateGSTNumber(validGSTNumber),
                "Valid GST numbers should be accepted");
    }

    @Property(tries = 100)
    void emptyGSTNumberIsAccepted() {
        // GST number is optional, so empty should be valid
        assertDoesNotThrow(() -> validationUtil.validateGSTNumber(""),
                "Empty GST number should be accepted as it's optional");
        assertDoesNotThrow(() -> validationUtil.validateGSTNumber(null),
                "Null GST number should be accepted as it's optional");
    }

    // Generators

    @Provide
    Arbitrary<String> invalidEmails() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("notanemail"),
                Arbitraries.just("@example.com"),
                Arbitraries.just("user@"),
                Arbitraries.just("user @example.com"),
                Arbitraries.just("user@.com"),
                Arbitraries.just("user@example"),
                Arbitraries.just("user..name@example.com"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .filter(s -> !s.contains("@"))
        );
    }

    @Provide
    Arbitrary<String> validEmails() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.of("com", "org", "net", "edu", "gov")
        ).as((user, domain, tld) -> user + "@" + domain + "." + tld);
    }

    @Provide
    Arbitrary<String> invalidFormats() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("pdf"),
                Arbitraries.just("json"),
                Arbitraries.just("xml"),
                Arbitraries.just("txt"),
                Arbitraries.just("doc"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .filter(s -> !s.equalsIgnoreCase("excel") && !s.equalsIgnoreCase("csv"))
        );
    }

    @Provide
    Arbitrary<String> validFormats() {
        return Arbitraries.of("excel", "csv", "Excel", "CSV", "EXCEL", "Csv", " excel ", " csv ");
    }

    @Provide
    Arbitrary<String> userIds() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> invalidGSTNumbers() {
        return Arbitraries.oneOf(
                // Too short
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10),
                // Too long
                Arbitraries.strings().alpha().numeric().ofMinLength(20).ofMaxLength(30),
                // Wrong format - all numbers
                Arbitraries.strings().numeric().ofLength(15),
                // Wrong format - all letters
                Arbitraries.strings().alpha().ofLength(15),
                // Missing Z
                Arbitraries.just("22AAAAA0000A1A5"),
                // Wrong structure
                Arbitraries.just("AAAAA22000A1Z5A")
        );
    }

    @Provide
    Arbitrary<String> validGSTNumbers() {
        // Valid GST format: 2 digits + 5 letters + 4 digits + 1 letter + 1 alphanumeric + Z + 1 alphanumeric
        return Combinators.combine(
                Arbitraries.strings().numeric().ofLength(2),
                Arbitraries.strings().alpha().ofLength(5).map(String::toUpperCase),
                Arbitraries.strings().numeric().ofLength(4),
                Arbitraries.strings().alpha().ofLength(1).map(String::toUpperCase),
                Arbitraries.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C"),
                Arbitraries.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C")
        ).as((d1, l1, d2, l2, a1, a2) -> d1 + l1 + d2 + l2 + a1 + "Z" + a2);
    }
}
