package net.hardnorth.github.merge.service.impl;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;
import net.hardnorth.github.merge.exception.NotFoundException;
import net.hardnorth.github.merge.service.EncryptionService;
import net.hardnorth.github.merge.service.SecretManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static java.util.Optional.ofNullable;

public class TinkEncryptionService implements EncryptionService {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private final KeysetHandle keysetHandle;
    private final Aead aead;

    public TinkEncryptionService(SecretManager secretManager, String keyName) throws GeneralSecurityException {
        AeadConfig.register();

        String key;
        try {
            key = secretManager.getSecret(keyName);
        } catch (NotFoundException ignore) {
            key = null;
        }
        keysetHandle = ofNullable(key).map(k -> {
            try {
                try {
                    return CleartextKeysetHandle.read(JsonKeysetReader.withString(k));
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // Unreal case, because we write keys to byte arrays
            }
        }).orElseGet(() -> {
            KeyTemplate keysetTemplate = AesGcmKeyManager.aes128GcmTemplate();
            KeysetHandle kh;
            try {
                kh = KeysetHandle.generateNew(keysetTemplate);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
            String k;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CleartextKeysetHandle.write(kh, JsonKeysetWriter.withOutputStream(baos));
                k = baos.toString(CHARSET);
            } catch (IOException e) {
                throw new RuntimeException(e); // Unreal case, because we write keys to byte arrays
            }
            secretManager.saveSecret(keyName, k);
            return kh;
        });
        aead = keysetHandle.getPrimitive(Aead.class);
    }

    @Nonnull
    @Override
    public byte[] encrypt(@Nonnull byte[] data, @Nullable byte[] associatedData) {
        try {
            return aead.encrypt(data, associatedData);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nonnull
    @Override
    public byte[] decrypt(@Nonnull byte[] data, @Nullable byte[] associatedData) {
        try {
            return aead.decrypt(data, associatedData);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
