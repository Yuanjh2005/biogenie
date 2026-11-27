package com.agent.biogenie.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256 password hashing with a per-account UUID salt. */
public final class PasswordHashUtil {

    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;

    private PasswordHashUtil() {}

    public static String newSalt() {
        // UUID is required by the account format; randomUUID uses a cryptographically strong PRNG.
        return UUID.randomUUID().toString();
    }

    public static String hash(char[] password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt.getBytes(StandardCharsets.UTF_8), ITERATIONS, KEY_LENGTH_BITS);
        try {
            return Base64.getEncoder().encodeToString(
                    SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean matches(char[] password, String salt, String expectedHash) {
        if (salt == null || expectedHash == null) return false;
        byte[] actual = hash(password, salt).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
