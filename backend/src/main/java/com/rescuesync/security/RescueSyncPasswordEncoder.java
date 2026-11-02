package com.rescuesync.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Compatible password encoder for RescueSync.
 * 
 * Interoperates with the original Java Swing desktop application:
 * - Format: PBKDF2$iterations$base64Salt$base64Hash
 * - Algorithm: PBKDF2WithHmacSHA256 (NIST SP 800-63B compliant)
 * - Iterations: 65,536
 * - Salt: 16 bytes cryptographically secure random
 * - Key length: 256 bits (32 bytes)
 * - Constant-time verification with MessageDigest.isEqual
 * 
 * Also supports BCrypt hashes ($2a$, $2b$) and legacy plaintext admin passwords
 * for seamless backward compatibility without resetting existing credentials.
 */
@Component
public class RescueSyncPasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null || rawPassword.length() == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toString().toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return String.format("PBKDF2$%d$%s$%s", ITERATIONS, saltB64, hashB64);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }

        // 1. Primary format: PBKDF2$iterations$salt$hash (Desktop and Web standard)
        if (encodedPassword.startsWith("PBKDF2$")) {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

                byte[] actualHash = pbkdf2(rawPassword.toString().toCharArray(), salt, iterations, expectedHash.length * 8);
                return MessageDigest.isEqual(actualHash, expectedHash);
            } catch (Exception e) {
                return false;
            }
        }

        // 2. BCrypt format support ($2a$, $2b$, $2y$)
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(rawPassword.toString(), encodedPassword);
        }

        // 3. Constant-time fallback for legacy plaintext evaluation credentials (e.g. admin123)
        return MessageDigest.isEqual(rawPassword.toString().getBytes(), encodedPassword.getBytes());
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        // Upgrade legacy plaintext or weak hashes to PBKDF2
        return encodedPassword != null && !encodedPassword.startsWith("PBKDF2$");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic key derivation error: " + e.getMessage(), e);
        }
    }
}

