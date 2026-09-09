package com.kfpd.cloud.common.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EncryptedDataSourceValuesTests {

    private static final String SECRET = "cloud-family-demo-datasource-crypto-secret";
    private static final String ENCRYPTED_ROOT = "ENC(U0rj/0TFqN/ikdyYo1QcsZa9APbjTkdLYLth5ycDEk4=)";

    @Test
    void decryptsEncryptedValue() {
        assertThat(EncryptedDataSourceValues.decryptIfNecessary(ENCRYPTED_ROOT, SECRET)).isEqualTo("root");
    }

    @Test
    void returnsPlainValueUnchanged() {
        assertThat(EncryptedDataSourceValues.decryptIfNecessary("admin", SECRET)).isEqualTo("admin");
    }

    @Test
    void encryptsValueThatCanBeDecrypted() {
        String encrypted = EncryptedDataSourceValues.encrypt("root", SECRET);

        assertThat(encrypted).startsWith("ENC(").endsWith(")");
        assertThat(EncryptedDataSourceValues.decryptIfNecessary(encrypted, SECRET)).isEqualTo("root");
    }
}
