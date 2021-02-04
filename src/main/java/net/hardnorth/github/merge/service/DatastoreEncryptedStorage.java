package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.*;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class DatastoreEncryptedStorage implements EncryptedStorage {

    private static final String STORAGE_KIND = "encrypted-storage";
    private static final String ENCRYPTED_VALUE = "encryptedValue";
    private static final String VALUE_KEY = "valueKey";
    private static final String INITIALIZATION_VECTOR = "iv";

    private final Datastore datastore;
    private final KeysetHandle keySet;
    private final Aead aead;
    private final KeyFactory tokenKeyFactory;

    public DatastoreEncryptedStorage(Datastore datastoreService, byte[] encryptionKey) throws GeneralSecurityException {
        datastore = datastoreService;
        tokenKeyFactory = datastore.newKeyFactory().setKind(STORAGE_KIND);
        AeadConfig.register();
        keySet = KeysetHandle.readNoSecret(encryptionKey);
        aead = keySet.getPrimitive(Aead.class);
    }

    @Override
    @Nullable
    public String getValue(@Nonnull String key) {
        EntityQuery query = Query.newEntityQueryBuilder()
                .setKind(STORAGE_KIND)
                .setFilter(
                        StructuredQuery.CompositeFilter.and(
                                StructuredQuery.PropertyFilter.eq(VALUE_KEY, key)
                        )
                ).build();
        QueryResults<Entity> result = datastore.run(query);
        if(!result.hasNext()) {
            return null;
        }
        Entity value = result.next();
        String encrypted = value.getString(ENCRYPTED_VALUE);
        String iv = value.getString(INITIALIZATION_VECTOR);
        Base64.Decoder decoder = Base64.getDecoder();
        try {
            return new String(aead.decrypt(decoder.decode(encrypted), decoder.decode(iv)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void saveValue(String key, String value) {

    }
}
