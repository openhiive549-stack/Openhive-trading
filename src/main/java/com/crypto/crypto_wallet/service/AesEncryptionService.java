package com.crypto.crypto_wallet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM symmetric encryption/decryption service.
 *
 * Encrypt: plaintext  →  Base64(IV + ciphertext + auth-tag)
 * Decrypt: Base64(IV + ciphertext + auth-tag)  →  plaintext
 *
 * The 32-byte hex key is loaded from the MASTER_KEY_SECRET environment variable.
 */
@Service
public class AesEncryptionService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12;   // 96-bit IV (NIST recommended)
    private static final int    GCM_TAG    = 128;  // 128-bit auth tag

    private final SecretKey secretKey;

    public AesEncryptionService(@Value("${master.key.secret}") String secretString) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secretString.getBytes("UTF-8"));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES key from secret string", e);
        }
    }

    /**
     * Encrypts a plain-text string.
     * @return Base64-encoded string: [12-byte IV | ciphertext | 16-byte GCM tag]
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG, iv));

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Prepend IV so we can recover it on decrypt
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv,          0, combined, 0,         iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded string produced by {@link #encrypt(String)}.
     * @return original plaintext
     */
    public String decrypt(String base64Ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Ciphertext);

            byte[] iv          = new byte[GCM_IV_LEN];
            byte[] cipherBytes = new byte[combined.length - GCM_IV_LEN];
            System.arraycopy(combined, 0,         iv,          0, GCM_IV_LEN);
            System.arraycopy(combined, GCM_IV_LEN, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG, iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────
}
