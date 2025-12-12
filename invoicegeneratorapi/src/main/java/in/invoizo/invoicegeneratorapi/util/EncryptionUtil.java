package in.invoizo.invoicegeneratorapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting sensitive data using AES-256-GCM.
 * This is used to encrypt Razorpay credentials before storing them in the database.
 */
@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    @Value("${encryption.secret.key:}")
    private String encryptionSecretKey;

    /**
     * Encrypts the given plain text using AES-256-GCM encryption.
     * 
     * @param plainText The text to encrypt
     * @return Base64 encoded encrypted text with IV prepended
     * @throws Exception if encryption fails
     */
    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Plain text cannot be null or empty");
        }

        SecretKey secretKey = getSecretKey();
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        // Encrypt the data
        byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);

        // Encode to Base64 for storage
        String encrypted = Base64.getEncoder().encodeToString(byteBuffer.array());
        log.debug("Successfully encrypted data");
        
        return encrypted;
    }

    /**
     * Decrypts the given encrypted text using AES-256-GCM decryption.
     * 
     * @param encryptedText Base64 encoded encrypted text with IV prepended
     * @return Decrypted plain text
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }

        SecretKey secretKey = getSecretKey();

        // Decode from Base64
        byte[] decodedData = Base64.getDecoder().decode(encryptedText);

        // Extract IV and encrypted data
        ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] encryptedData = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedData);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        // Decrypt the data
        byte[] decryptedData = cipher.doFinal(encryptedData);
        String decrypted = new String(decryptedData, StandardCharsets.UTF_8);
        
        log.debug("Successfully decrypted data");
        
        return decrypted;
    }

    /**
     * Gets or generates the secret key for encryption/decryption.
     * In production, this should be loaded from a secure key management system.
     * 
     * @return SecretKey for AES encryption
     */
    private SecretKey getSecretKey() throws Exception {
        if (encryptionSecretKey != null && !encryptionSecretKey.isEmpty()) {
            // Use configured key
            byte[] decodedKey = Base64.getDecoder().decode(encryptionSecretKey);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } else {
            // Generate a new key (for development/testing only)
            log.warn("No encryption key configured. Generating a temporary key. " +
                    "This should not be used in production!");
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE);
            return keyGenerator.generateKey();
        }
    }

    /**
     * Generates a new AES-256 key and returns it as a Base64 encoded string.
     * This can be used to generate a key for configuration.
     * 
     * @return Base64 encoded AES-256 key
     */
    public static String generateNewKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}
