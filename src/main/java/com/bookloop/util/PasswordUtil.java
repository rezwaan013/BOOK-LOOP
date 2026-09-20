package com.bookloop.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Password hashing using SHA-256 with a random salt.
 * No external libraries required.
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    /** Generates a random salt string using UUID (128 bits of randomness). */
    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Returns a hex-encoded SHA-256 digest of {@code salt + password + salt}.
     * @param password plain-text password
     * @param salt     random salt (from generateSalt)
     */
    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((salt + password + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash + salt.
     * @return true if the password produces the same hash
     */
    public static boolean verify(String password, String salt, String storedHash) {
        return hash(password, salt).equals(storedHash);
    }
}
