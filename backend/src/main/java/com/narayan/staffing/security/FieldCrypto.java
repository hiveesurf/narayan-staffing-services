package com.narayan.staffing.security;

import com.narayan.staffing.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class FieldCrypto {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public FieldCrypto(AppProperties properties) {
        try {
            String secret = properties.getSecurity().getSensitiveDataKey();
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("SENSITIVE_DATA_KEY is required");
            }
            this.key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not initialise field encryption", ex);
        }
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt value", ex);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(stored);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decrypt value", ex);
        }
    }
}
