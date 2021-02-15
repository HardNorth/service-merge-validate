package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.exception.NotFoundException;
import net.hardnorth.github.merge.service.impl.TinkEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class TinkEncryptionServiceTest {

    private static final String TEST_ENCRYPTION_KEY = "test-encryption-key";

    private static final String TEST_ENCRYPTION_KEY_VALUE =
            "{ \"primaryKeyId\": 544382890, \"key\": [{ \"keyData\": { \"typeUrl\": \"type.googleapis.com/google.crypto.tink.AesGcmKey\", \"keyMaterialType\": \"SYMMETRIC\", \"value\": \"GhCPSBzwNuYEZFUI5VEogbrY\" }, \"outputPrefixType\": \"TINK\", \"keyId\": 544382890, \"status\": \"ENABLED\" }] }";

    private static final String TEST_ENCRYPTION_DATA = UUID.randomUUID().toString();
    private static final String TEST_ENCRYPTION_ADDED_DATA = UUID.randomUUID().toString();

    private final SecretManager secretManager = mock(SecretManager.class);

    private EncryptionService encryptionService;

    @BeforeEach
    public void setup() throws GeneralSecurityException {
        encryptionService = new TinkEncryptionService(secretManager, TEST_ENCRYPTION_KEY);
    }

    @Test
    public void test_secret_manager_encrypt_new_key() {
        when(secretManager.getSecret(same(TEST_ENCRYPTION_KEY))).thenThrow(new NotFoundException("Not found"));

        byte[] result = encryptionService.encrypt(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8),
                TEST_ENCRYPTION_ADDED_DATA.getBytes(StandardCharsets.UTF_8));

        assertThat(result, notNullValue());
        assertThat(result.length, greaterThanOrEqualTo(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8).length));
        assertThat(result, not(equalTo(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8))));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(secretManager).saveSecret(same(TEST_ENCRYPTION_KEY), captor.capture());

        verify(secretManager).getSecret(any());
        String secret = captor.getValue();
        assertThat(secret, not(emptyOrNullString()));
    }

    @Test
    public void test_secret_manager_encrypt_old_key() {
        when(secretManager.getSecret(same(TEST_ENCRYPTION_KEY))).thenReturn(TEST_ENCRYPTION_KEY_VALUE);

        byte[] result = encryptionService.encrypt(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8),
                TEST_ENCRYPTION_ADDED_DATA.getBytes(StandardCharsets.UTF_8));

        assertThat(result, notNullValue());
        assertThat(result.length, greaterThanOrEqualTo(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8).length));
        assertThat(result, not(equalTo(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8))));

        verify(secretManager).getSecret(any());
        verify(secretManager, never()).saveSecret(any(), any());
    }

    @Test
    public void test_secret_manager_decrypt_old_key() {
        when(secretManager.getSecret(same(TEST_ENCRYPTION_KEY))).thenReturn(TEST_ENCRYPTION_KEY_VALUE);

        byte[] encrypted = encryptionService.encrypt(TEST_ENCRYPTION_DATA.getBytes(StandardCharsets.UTF_8),
                TEST_ENCRYPTION_ADDED_DATA.getBytes(StandardCharsets.UTF_8));

        byte[] decrypted = encryptionService.decrypt(encrypted,
                TEST_ENCRYPTION_ADDED_DATA.getBytes(StandardCharsets.UTF_8));

        assertThat(new String(decrypted, StandardCharsets.UTF_8), equalTo(TEST_ENCRYPTION_DATA));
        verify(secretManager).getSecret(any());
    }
}
