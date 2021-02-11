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
import java.util.concurrent.Callable;

import static java.util.Optional.ofNullable;

public class TinkEncryptionService implements EncryptionService {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private final EncryptionSupplier supplier;

    // lazy init encryption algorithm supplier
    private static class EncryptionSupplier implements Callable<Aead> {
        private volatile Aead aead;

        private final SecretManager secrets;
        private final String keyName;

        @SuppressWarnings("CdiInjectionPointsInspection")
        public EncryptionSupplier(SecretManager secretManager, String keyName) {
            secrets = secretManager;
            this.keyName = keyName;
        }

        @Override
        public Aead call() throws GeneralSecurityException {
            if (aead != null) {
                return aead;
            }
            synchronized (this) {
                if (aead != null) {
                    return aead;
                }

                String key;
                try {
                    key = secrets.getSecret(keyName);
                } catch (NotFoundException ignore) {
                    key = null;
                }

                KeysetHandle keysetHandle = ofNullable(key).map(k -> {
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
                    secrets.saveSecret(keyName, k);
                    return kh;
                });
                aead = keysetHandle.getPrimitive(Aead.class);
            }
            return aead;
        }
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    public TinkEncryptionService(SecretManager secretManager, String keyName) throws GeneralSecurityException {
        AeadConfig.register();
        supplier = new EncryptionSupplier(secretManager, keyName);
    }

    @Nonnull
    @Override
    public byte[] encrypt(@Nonnull byte[] data, @Nullable byte[] associatedData) {
        try {
            return supplier.call().encrypt(data, associatedData);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nonnull
    @Override
    public byte[] decrypt(@Nonnull byte[] data, @Nullable byte[] associatedData) {
        try {
            return supplier.call().decrypt(data, associatedData);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
