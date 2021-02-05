package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.exception.NotFoundException;

import javax.annotation.Nonnull;
import java.util.List;

public interface SecretManager {

    @Nonnull
    List<String> getSecrets(String... names);

    @Nonnull
    default String getSecret(String name) {
        List<String> secrets = getSecrets(name);
        if (secrets.isEmpty()) {
            throw new NotFoundException("Unable to get secret with name: " + name);
        }
        return secrets.get(0);
    }

    void saveSecret(String name, String data);
}
