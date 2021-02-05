package net.hardnorth.github.merge.service;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import static java.util.Optional.ofNullable;

public class DatastoreEncryptedStorage implements EncryptedStorage {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String STORAGE_KIND = "encrypted-storage";
    private static final String ENCRYPTED_VALUE = "encryptedValue";
    private static final String VALUE_KEY = "valueKey";
    private static final String CREATION_DATE = "creationDate";
    private static final String ACCESS_DATE = "accessDate";

    private final Datastore datastore;
    private final KeysetHandle keysetHandle;
    private final Aead aead;
    private final KeyFactory tokenKeyFactory;

    public DatastoreEncryptedStorage(Datastore datastoreService) throws GeneralSecurityException {
        datastore = datastoreService;
        tokenKeyFactory = datastore.newKeyFactory().setKind(STORAGE_KIND);
        AeadConfig.register();
        KeyTemplate keysetTemplate = AesGcmKeyManager.aes128GcmTemplate();
        keysetHandle = KeysetHandle.generateNew(keysetTemplate);
        aead = keysetHandle.getPrimitive(Aead.class);
    }

    public DatastoreEncryptedStorage(Datastore datastoreService, String encryptionKey) throws GeneralSecurityException {
        datastore = datastoreService;
        tokenKeyFactory = datastore.newKeyFactory().setKind(STORAGE_KIND);
        AeadConfig.register();
        try {
            keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(encryptionKey));
        } catch (IOException e) {
            throw new RuntimeException(e); // Unreal case, because we read keys from strings
        }
        aead = keysetHandle.getPrimitive(Aead.class);
    }

    @Override
    @Nullable
    public String getValue(@Nullable String authKey, @Nonnull String key) {
        EntityQuery query = Query.newEntityQueryBuilder()
                .setKind(STORAGE_KIND)
                .setFilter(
                        StructuredQuery.CompositeFilter.and(
                                StructuredQuery.PropertyFilter.eq(VALUE_KEY, key)
                        )
                ).build();
        QueryResults<Entity> result = datastore.run(query);
        if (!result.hasNext()) {
            return null;
        }
        Entity value = result.next();
        String encrypted = value.getString(ENCRYPTED_VALUE);
        Base64.Decoder decoder = Base64.getDecoder();
        try {
            return new String(aead.decrypt(decoder.decode(encrypted), ofNullable(authKey).map(k -> k.getBytes(CHARSET)).orElse(null)), CHARSET);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            Entity valueAccess = Entity.newBuilder(value)
                    .set(ACCESS_DATE, Timestamp.now())
                    .build();
            datastore.put(valueAccess);
        }
    }

    @Override
    public void saveValue(@Nullable String authKey, @Nonnull String key, @Nonnull String value) {
        Key storageKey = datastore.allocateId(tokenKeyFactory.newKey());
        Timestamp creationDate = Timestamp.now();
        try {
            String encryptedValue = Base64.getEncoder()
                    .encodeToString(aead.encrypt(value.getBytes(CHARSET), ofNullable(authKey).map(k -> k.getBytes(CHARSET)).orElse(null)));
            Entity storage = Entity.newBuilder(storageKey)
                    .set(VALUE_KEY, key)
                    .set(ENCRYPTED_VALUE, encryptedValue)
                    .set(CREATION_DATE, creationDate)
                    .set(ACCESS_DATE, creationDate)
                    .build();
            datastore.add(storage);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nonnull
    @Override
    public String getEncryptionKey() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(baos));
            return baos.toString(CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(e); // Unreal case, because we write keys to byte arrays
        }
    }
}
