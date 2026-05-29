package com.mekylei.transactionprocessing.compartilhado.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
public class CriptografiaConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final byte[] chaveBytes;

    public CriptografiaConverter(@Value("${app.criptografia.chave}") String chaveBase64) {
        this.chaveBytes = Base64.getDecoder().decode(chaveBase64);
        if (this.chaveBytes.length != 32) {
            throw new IllegalArgumentException("A chave decodificada deve ter 32 bytes (256 bits) em Base64.");
        }
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(chaveBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Formato armazenado: Base64(IV || ciphertext)
            byte[] combined = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH_BYTES, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar valor", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String encrypted) {
        if (encrypted == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(chaveBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao descriptografar valor", e);
        }
    }
}