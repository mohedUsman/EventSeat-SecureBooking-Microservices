package com.eventseat.identity.jpa;

import com.eventseat.identity.config.PiiKeyHolder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-GCM String <-> DB column converter.
 * Stores Base64(iv):Base64(cipherTextWithTag)
 * - iv: 12 bytes
 * - tag length: 128 bits (default for GCM)
 */
@Converter(autoApply = false)
public class AesGcmStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank())
            return null;
        SecretKey key = PiiKeyHolder.getKey();
        if (key == null) {
            // As a safety, store a marker rather than plain text
            return "ENC-ERROR:NO-KEY";
        }
        try {
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String ctB64 = Base64.getEncoder().encodeToString(cipherText);
            return ivB64 + ":" + ctB64;
        } catch (Exception e) {
            // Do not leak the plaintext; persist a recognizable failure marker
            return "ENC-ERROR:" + e.getClass().getSimpleName();
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank())
            return null;
        if (dbData.startsWith("ENC-ERROR:"))
            return null; // unreadable, treat as null
        SecretKey key = PiiKeyHolder.getKey();
        if (key == null) {
            // Cannot decrypt without key
            return null;
        }
        try {
            int sep = dbData.indexOf(':');
            if (sep < 0)
                return null;
            String ivB64 = dbData.substring(0, sep);
            String ctB64 = dbData.substring(sep + 1);
            byte[] iv = Base64.getDecoder().decode(ivB64);
            byte[] cipherText = Base64.getDecoder().decode(ctB64);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // On any error, don't propagate secrets
            return null;
        }
    }
}
