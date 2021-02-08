package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
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
    private static final String VALUE_KEY = "valueKey";
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
            return new String(encryption.decrypt(decoder.decode(encrypted), ofNullable(authKey).map(k -> k.getBytes(CHARSET)).orElse(null)), CHARSET);
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
        String encryptedValue = Base64.getEncoder()
                .encodeToString(encryption.encrypt(value.getBytes(CHARSET), ofNullable(authKey).map(k -> k.getBytes(CHARSET)).orElse(null)));
        Entity storage = Entity.newBuilder(storageKey)
                .set(VALUE_KEY, key)
                .set(ENCRYPTED_VALUE, encryptedValue)
                .set(CREATION_DATE, creationDate)
                .set(ACCESS_DATE, creationDate)
                .build();
        datastore.add(storage);
    }
}
