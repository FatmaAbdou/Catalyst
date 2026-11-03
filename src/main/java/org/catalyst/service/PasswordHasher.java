package org.catalyst.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Stores passwords as salted PBKDF2 hashes rather than reversible text. */
public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return "pbkdf2_sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean verify(char[] password, String encodedHash) {
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            throw new IllegalStateException("The saved password hash has an unsupported format.");
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 1 || iterations > 2_000_000) {
                throw new IllegalStateException("The saved password hash has invalid parameters.");
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = derive(password, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("The saved password hash is invalid.", exception);
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}
