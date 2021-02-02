package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;
import java.util.List;

public interface SecretManager {

    @Nonnull
    List<String> getSecrets(String... names);

}
