package net.hardnorth.github.merge.service;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import net.hardnorth.github.merge.exception.ConnectionException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GoogleSecretManager implements SecretManager {

    private static final String VERSION_ID = "latest";

    private final String projectName;

    public GoogleSecretManager(String projectId) {
        projectName = projectId;
    }

    @Override
    @Nonnull
    public List<String> getSecrets(String... names) {
        if (names == null) {
            return Collections.emptyList();
        }
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            return Arrays.stream(names)
                    .map(n -> client.accessSecretVersion(SecretVersionName.of(projectName, n, VERSION_ID)).getPayload().getData().toStringUtf8())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage(), e);
        }
    }
}
