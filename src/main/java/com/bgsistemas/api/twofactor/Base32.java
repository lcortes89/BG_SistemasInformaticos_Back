package com.bgsistemas.api.twofactor;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

// BGSI-276: authenticator apps (Google/Microsoft Authenticator) read the secret in Base32 (RFC 4648)
final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {
    }

    static String encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                result.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            result.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        }
        return result.toString();
    }

    static byte[] decode(String text) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : text.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT).toCharArray()) {
            int value = ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Not a Base32 text");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                result.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return result.toByteArray();
    }
}
