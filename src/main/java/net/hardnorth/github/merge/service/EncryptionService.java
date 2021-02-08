package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EncryptionService {
    @Nonnull
    byte[] encrypt(@Nonnull byte[] data, @Nullable byte[] associatedData);

    @Nonnull
    byte[] decrypt(@Nonnull byte[] data, @Nullable byte[] associatedData);
}
