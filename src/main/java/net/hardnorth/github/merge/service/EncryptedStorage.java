package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EncryptedStorage {

    @Nullable
    String getValue(@Nonnull String key, @Nullable String authKey);

    void saveValue(@Nonnull String key, @Nonnull String value, @Nullable String authKey);
}
