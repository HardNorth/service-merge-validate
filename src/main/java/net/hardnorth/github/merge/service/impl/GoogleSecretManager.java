package net.hardnorth.github.merge.service.impl;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.ByteString;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.exception.NotFoundException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.SecretManager;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GoogleSecretManager implements SecretManager {

    private static final String VERSION_ID = "latest";

    private final String projectName;
    private final java.nio.charset.Charset charset;

    public GoogleSecretManager(String projectId, Charset serviceCharset)
    {
        projectName = projectId;
        charset = serviceCharset.get();
    }

    @Override
    @Nonnull
    public List<byte[]> getRawSecrets(@Nullable String... names) {
        if (names == null) {
            return Collections.emptyList();
        }
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            return Arrays.stream(names)
                    .map(n -> {
                        try {
                            return client.accessSecretVersion(SecretVersionName.of(projectName, n, VERSION_ID))
                                    .getPayload().getData().toByteArray();
                        } catch (ApiException e) {
                            if (HttpStatus.SC_NOT_FOUND == e.getStatusCode().getCode().getHttpStatusCode()) {
                                throw new NotFoundException("Secret not found: " + n);
                            } else {
                                throw e;
                            }
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage(), e);
        }
    }

    @Override
    @Nonnull
    public List<String> getSecrets(@Nullable String... names) {
        return getRawSecrets(names).stream().map(s->new String(s, charset)).collect(Collectors.toList());
    }

    @Override
    public void saveSecret(String name, String data) {
        SecretName secretName = SecretName.of(projectName, name);
        SecretPayload payload =
                SecretPayload.newBuilder()
                        .setData(ByteString.copyFromUtf8(data))
                        .build();


        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            try {
                client.getSecret(secretName);
            } catch (ApiException e) {
                if (HttpStatus.SC_NOT_FOUND == e.getStatusCode().getCode().getHttpStatusCode()) {
                    Secret secret =
                            Secret.newBuilder()
                                    .setReplication(
                                            Replication.newBuilder()
                                                    .setAutomatic(Replication.Automatic.newBuilder().build())
                                                    .build())
                                    .build();
                    client.createSecret(ProjectName.of(projectName), name, secret);
                } else {
                    throw new IllegalStateException(e);
                }
            }
            client.addSecretVersion(secretName, payload);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage(), e);
        }
    }

}
