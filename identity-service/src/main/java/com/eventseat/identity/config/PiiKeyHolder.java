package com.eventseat.identity.config;

import javax.crypto.SecretKey;

/**
 * Static holder to make the AES-GCM key accessible from JPA
 * AttributeConverters,
 * which are instantiated by JPA and not Spring-managed.
 */
public final class PiiKeyHolder {
    private static volatile SecretKey key;

    private PiiKeyHolder() {
    }

    public static void setKey(SecretKey k) {
        key = k;
    }

    public static SecretKey getKey() {
        return key;
    }
}
