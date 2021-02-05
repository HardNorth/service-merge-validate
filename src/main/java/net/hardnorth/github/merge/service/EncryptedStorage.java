package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EncryptedStorage {

    @Nullable
    String getValue(@Nullable String authKey, @Nonnull String key);

    void saveValue(@Nullable String authKey, @Nonnull String key, @Nonnull String value);

    @Nonnull
    String getEncryptionKey();
}
