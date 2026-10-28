package com.bgsistemas.api.twofactor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// BGSI-276: the TOTP secret is never stored in plain text
class SecretCipherTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String OTHER_KEY = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private final SecretCipher cipher = new SecretCipher(KEY);

    @Test
    void encryptsAndDecrypts() {
        String stored = cipher.encrypt("GEZDGNBVGY3TQOJQ");

        assertThat(stored).doesNotContain("GEZDGNBVGY3TQOJQ");
        assertThat(cipher.decrypt(stored)).isEqualTo("GEZDGNBVGY3TQOJQ");
    }

    @Test
    void theSameSecretIsEncryptedDifferentlyEachTime() {
        assertThat(cipher.encrypt("GEZDGNBVGY3TQOJQ")).isNotEqualTo(cipher.encrypt("GEZDGNBVGY3TQOJQ"));
    }

    @Test
    void anotherKeyCannotReadIt() {
        String stored = cipher.encrypt("GEZDGNBVGY3TQOJQ");

        assertThatThrownBy(() -> new SecretCipher(OTHER_KEY).decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theKeyMustBe32Bytes() {
        assertThatThrownBy(() -> new SecretCipher("c2hvcnQ=")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOTP_ENCRYPTION_KEY");
    }
}
