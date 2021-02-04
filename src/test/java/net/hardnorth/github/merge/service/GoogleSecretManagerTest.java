package net.hardnorth.github.merge.service;

import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.secretmanager.v1.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GoogleSecretManagerTest {

    private static final String PROJECT_NAME_STR = "test";
    private static final String SECRET_NAME_STR = "test-secret";
    private static final SecretName SECRET_NAME = SecretName.of(PROJECT_NAME_STR, SECRET_NAME_STR);
    private static final ProjectName PROJECT_NAME = ProjectName.of(PROJECT_NAME_STR);

    private final SecretManagerServiceClient secretClient = mock(SecretManagerServiceClient.class);
    private final Secret secret = mock(Secret.class);
    private final SecretManager secretManager = new GoogleSecretManager(PROJECT_NAME_STR);
    private final String secretKey = UUID.randomUUID().toString();

    @Test
    public void test_secret_manager_save_secret_already_exists() {
        when(secretClient.getSecret(any(SecretName.class))).thenReturn(secret);
        try (MockedStatic<SecretManagerServiceClient> theMock = Mockito.mockStatic(SecretManagerServiceClient.class)) {
            theMock.when(SecretManagerServiceClient::create).thenReturn(secretClient);
            secretManager.saveSecret("test-secret", secretKey);
        }
        verify(secretClient).getSecret(eq(SECRET_NAME));
        ArgumentCaptor<SecretPayload> payloadCapture = ArgumentCaptor.forClass(SecretPayload.class);
        verify(secretClient).addSecretVersion(eq(SECRET_NAME), payloadCapture.capture());
        assertThat(payloadCapture.getValue().getData().toStringUtf8(), equalTo(secretKey));

        verify(secretClient, never()).createSecret(any(ProjectName.class), any(), any());
        verify(secretClient, never()).createSecret(any(String.class), any(), any());
        verify(secretClient, never()).createSecret(any(CreateSecretRequest.class));
    }

    @Test
    public void test_secret_manager_save_secret_not_already_exists() {
        StatusCode statusCode = mock(StatusCode.class);
        when(statusCode.getCode()).thenReturn(StatusCode.Code.NOT_FOUND);

        when(secretClient.getSecret(any(SecretName.class))).thenThrow(new NotFoundException(new RuntimeException(), statusCode, false));
        try (MockedStatic<SecretManagerServiceClient> theMock = Mockito.mockStatic(SecretManagerServiceClient.class)) {
            theMock.when(SecretManagerServiceClient::create).thenReturn(secretClient);
            secretManager.saveSecret("test-secret", secretKey);
        }
        verify(secretClient).getSecret(eq(SECRET_NAME));
        ArgumentCaptor<Secret> secretCapture = ArgumentCaptor.forClass(Secret.class);
        verify(secretClient).createSecret(eq(PROJECT_NAME), same(SECRET_NAME_STR), secretCapture.capture());
        assertThat(secretCapture.getValue().getReplication(), notNullValue());

        ArgumentCaptor<SecretPayload> payloadCapture = ArgumentCaptor.forClass(SecretPayload.class);
        verify(secretClient).addSecretVersion(eq(SECRET_NAME), payloadCapture.capture());
        assertThat(payloadCapture.getValue().getData().toStringUtf8(), equalTo(secretKey));

        verify(secretClient, never()).createSecret(any(String.class), any(), any());
        verify(secretClient, never()).createSecret(any(CreateSecretRequest.class));
    }
}
