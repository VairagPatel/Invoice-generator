package in.invoizo.invoicegeneratorapi.util;

import net.jqwik.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for EncryptionUtil.
 * Tests universal properties that should hold across all valid inputs.
 */
class EncryptionUtilPropertyTest {

    /**
     * Feature: invoice-enhancements, Property 6: Credentials are encrypted
     * For any Razorpay API credentials stored in the database, the stored value
     * should not equal the plain text value.
     * Validates: Requirements 2.1, 5.4
     */
    @Property(tries = 100)
    void credentialsAreEncrypted(@ForAll("credentials") String plainTextCredential) throws Exception {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        
        // Set a test encryption key
        String testKey = EncryptionUtil.generateNewKey();
        ReflectionTestUtils.setField(encryptionUtil, "encryptionSecretKey", testKey);
        
        // Encrypt the credential
        String encryptedCredential = encryptionUtil.encrypt(plainTextCredential);
        
        // The encrypted value should not equal the plain text
        assertNotEquals(plainTextCredential, encryptedCredential,
                "Encrypted credential should not equal plain text");
        
        // The encrypted value should not be empty
        assertNotNull(encryptedCredential, "Encrypted credential should not be null");
        assertFalse(encryptedCredential.isEmpty(), "Encrypted credential should not be empty");
        
        // The encrypted value should be different each time (due to random IV)
        String encryptedCredential2 = encryptionUtil.encrypt(plainTextCredential);
        assertNotEquals(encryptedCredential, encryptedCredential2,
                "Each encryption should produce a different result due to random IV");
    }

    /**
     * Property: Encryption round-trip preserves data
     * For any credential, encrypting then decrypting should return the original value.
     */
    @Property(tries = 100)
    void encryptionRoundTrip(@ForAll("credentials") String plainTextCredential) throws Exception {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        
        // Set a test encryption key
        String testKey = EncryptionUtil.generateNewKey();
        ReflectionTestUtils.setField(encryptionUtil, "encryptionSecretKey", testKey);
        
        // Encrypt then decrypt
        String encrypted = encryptionUtil.encrypt(plainTextCredential);
        String decrypted = encryptionUtil.decrypt(encrypted);
        
        // Should get back the original value
        assertEquals(plainTextCredential, decrypted,
                "Decrypted value should equal original plain text");
    }

    /**
     * Property: Encryption with different keys produces different results
     * For any credential, encrypting with different keys should produce different encrypted values.
     */
    @Property(tries = 100)
    void differentKeysProduceDifferentResults(@ForAll("credentials") String plainTextCredential) throws Exception {
        EncryptionUtil encryptionUtil1 = new EncryptionUtil();
        EncryptionUtil encryptionUtil2 = new EncryptionUtil();
        
        // Set different encryption keys
        String testKey1 = EncryptionUtil.generateNewKey();
        String testKey2 = EncryptionUtil.generateNewKey();
        ReflectionTestUtils.setField(encryptionUtil1, "encryptionSecretKey", testKey1);
        ReflectionTestUtils.setField(encryptionUtil2, "encryptionSecretKey", testKey2);
        
        // Encrypt with both keys
        String encrypted1 = encryptionUtil1.encrypt(plainTextCredential);
        String encrypted2 = encryptionUtil2.encrypt(plainTextCredential);
        
        // Results should be different
        assertNotEquals(encrypted1, encrypted2,
                "Encryption with different keys should produce different results");
        
        // But each should decrypt correctly with its own key
        assertEquals(plainTextCredential, encryptionUtil1.decrypt(encrypted1));
        assertEquals(plainTextCredential, encryptionUtil2.decrypt(encrypted2));
    }

    /**
     * Property: Decryption with wrong key fails
     * For any credential, decrypting with a different key than used for encryption should fail.
     */
    @Property(tries = 100)
    void decryptionWithWrongKeyFails(@ForAll("credentials") String plainTextCredential) throws Exception {
        EncryptionUtil encryptionUtil1 = new EncryptionUtil();
        EncryptionUtil encryptionUtil2 = new EncryptionUtil();
        
        // Set different encryption keys
        String testKey1 = EncryptionUtil.generateNewKey();
        String testKey2 = EncryptionUtil.generateNewKey();
        ReflectionTestUtils.setField(encryptionUtil1, "encryptionSecretKey", testKey1);
        ReflectionTestUtils.setField(encryptionUtil2, "encryptionSecretKey", testKey2);
        
        // Encrypt with first key
        String encrypted = encryptionUtil1.encrypt(plainTextCredential);
        
        // Attempting to decrypt with second key should fail
        assertThrows(Exception.class, () -> {
            encryptionUtil2.decrypt(encrypted);
        }, "Decryption with wrong key should throw an exception");
    }

    /**
     * Property: Empty or null inputs are rejected
     */
    @Property(tries = 10)
    void emptyOrNullInputsAreRejected() throws Exception {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        String testKey = EncryptionUtil.generateNewKey();
        ReflectionTestUtils.setField(encryptionUtil, "encryptionSecretKey", testKey);
        
        // Null input should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.encrypt(null);
        }, "Encrypting null should throw IllegalArgumentException");
        
        // Empty input should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.encrypt("");
        }, "Encrypting empty string should throw IllegalArgumentException");
        
        // Null encrypted text should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.decrypt(null);
        }, "Decrypting null should throw IllegalArgumentException");
        
        // Empty encrypted text should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.decrypt("");
        }, "Decrypting empty string should throw IllegalArgumentException");
    }

    /**
     * Provides arbitrary credential strings for testing.
     * Generates realistic Razorpay-like credentials.
     */
    @Provide
    Arbitrary<String> credentials() {
        return Arbitraries.oneOf(
                // Razorpay key ID format: rzp_test_XXXX or rzp_live_XXXX
                Combinators.combine(
                        Arbitraries.of("rzp_test_", "rzp_live_"),
                        Arbitraries.strings().alpha().numeric().ofLength(16)
                ).as((prefix, suffix) -> prefix + suffix),
                
                // Razorpay key secret format: random alphanumeric string
                Arbitraries.strings().alpha().numeric().ofMinLength(20).ofMaxLength(40),
                
                // Generic credentials
                Arbitraries.strings().ascii().ofMinLength(10).ofMaxLength(100)
        );
    }
}
