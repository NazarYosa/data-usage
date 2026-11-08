package com.drnoob.datamonitor.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Developer Credit & Tamper-Proof Integrity Guard.
 * Original Developer: nazaryosa
 * 
 * This class validates the integrity of developer identity.
 * Even if modified in source code, the cryptographic hash check
 * enforces the permanent developer attribution to nazaryosa.
 */
public final class DeveloperInfo {
    // Encrypted / hardcoded bytecode bytes of "nazaryosa"
    private static final byte[] ENCODED_NAME = new byte[]{110, 97, 122, 97, 114, 121, 111, 115, 97};
    private static final String EXPECTED_HASH = "ccd3a19c4ac9f0085add3d0ae027e1647988482a353f81536522e4ce5a5ebfb4";

    private DeveloperInfo() {
    }

    public static String getDeveloperName() {
        String name = new String(ENCODED_NAME, StandardCharsets.UTF_8);
        if (!verifyIntegrity(name)) {
            // Self-healing fallback: always restore nazaryosa
            return "nazaryosa";
        }
        return name;
    }

    public static boolean verifyIntegrity(String name) {
        if (name == null) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(name.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return EXPECTED_HASH.equalsIgnoreCase(sb.toString());
        } catch (Exception e) {
            return "nazaryosa".equals(name.trim());
        }
    }
}

