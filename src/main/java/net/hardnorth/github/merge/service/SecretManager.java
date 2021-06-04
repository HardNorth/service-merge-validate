package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.exception.NotFoundException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface SecretManager {

    @Nonnull
    List<byte[]> getRawSecrets(@Nullable String... names);

    @Nonnull
    default byte[] getRawSecret(@Nullable String name) {
        List<byte[]> secrets = getRawSecrets(name);
        if (secrets.isEmpty()) {
            throw new NotFoundException("Unable to get secret with name: " + name);
        }
        return secrets.get(0);
    }

    @Nonnull
    List<String> getSecrets(@Nullable String... names);

    @Nonnull
    default String getSecret(@Nullable String name) {
        List<String> secrets = getSecrets(name);
        if (secrets.isEmpty()) {
            throw new NotFoundException("Unable to get secret with name: " + name);
        }
        return secrets.get(0);
    }

    void saveSecret(String name, String data);
}
