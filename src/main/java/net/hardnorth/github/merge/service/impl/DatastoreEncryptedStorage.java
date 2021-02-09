package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import net.hardnorth.github.merge.service.EncryptedStorage;
import net.hardnorth.github.merge.service.EncryptionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static java.util.Optional.ofNullable;

public class DatastoreEncryptedStorage implements EncryptedStorage {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String STORAGE_KIND = "encrypted-storage";
    private static final String ENCRYPTED_VALUE = "encryptedValue";
    private static final String CREATION_DATE = "creationDate";
    private static final String ACCESS_DATE = "accessDate";

    private final Datastore datastore;
    private final EncryptionService encryption;
    private final KeyFactory tokenKeyFactory;

    public DatastoreEncryptedStorage(Datastore datastoreService, EncryptionService encryptionService) {
        datastore = datastoreService;
        encryption = encryptionService;
        tokenKeyFactory = datastore.newKeyFactory().setKind(STORAGE_KIND);
    }

    @Override
    @Nullable
    public String getValue(@Nonnull String key, @Nullable String authKey) {
        Key datastoreKey = tokenKeyFactory.newKey(key);
        Entity result = datastore.get(datastoreKey);
        if (result == null) {
            return null;
        }
        String encrypted = result.getString(ENCRYPTED_VALUE);
        Base64.Decoder decoder = Base64.getDecoder();
        try {
            return new String(encryption.decrypt(decoder.decode(encrypted), ofNullable(authKey).map(k -> k.getBytes(CHARSET)).orElse(null)), CHARSET);
        } finally {
            Entity valueAccess = Entity.newBuilder(result)
                    .set(ACCESS_DATE, Timestamp.now())
                    .build();
            datastore.put(valueAccess);
        }
    }

    @Override
    public void saveValue(@Nonnull String key, @Nonnull String value, @Nullable String authKey) {
        Key storageKey = tokenKeyFactory.newKey(key);
        Timestamp creationDate = Timestamp.now();
        String encryptedValue = Base64.getEncoder()
                .encodeToString(encryption.encrypt(value.getBytes(CHARSET), ofNullable(authKey).map(k -> k.getBytes(CHARSET)).orElse(null)));
        Entity storage = Entity.newBuilder(storageKey)
                .set(ENCRYPTED_VALUE, encryptedValue)
                .set(CREATION_DATE, creationDate)
                .set(ACCESS_DATE, creationDate)
                .build();
        datastore.add(storage);
    }
}
