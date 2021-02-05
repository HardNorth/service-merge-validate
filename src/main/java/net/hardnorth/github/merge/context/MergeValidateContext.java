package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.exception.NotFoundException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.*;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class MergeValidateContext {

    @Produces
    @ApplicationScoped
    public Datastore datastoreService(@ConfigProperty(name = PropertyNames.APPLICATION_NAME) String applicationName) {
        return DatastoreOptions.newBuilder().setNamespace(applicationName).build().getService();
    }

    @Produces
    @ApplicationScoped
    public GithubOAuthService authorizationService(Datastore datastore, GithubClient githubApi, EncryptedStorage encryptedStorage,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_URL) String serviceUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_AUTHORIZE_URL) String githubOAuthUrl,
                                                   GithubCredentials credentials) {
        GithubOAuthService service = new GithubOAuthService(datastore, githubApi, encryptedStorage, serviceUrl, credentials);
        if (isNotBlank(githubOAuthUrl)) {
            service.setGithubOAuthUrl(githubOAuthUrl);
        }
        return service;
    }

    @Produces
    @ApplicationScoped
    public MergeValidateService mergeValidateService(Datastore datastore) {
        return new MergeValidateService(datastore);
    }

    @Produces
    @ApplicationScoped
    public GithubClient githubClientApi(@ConfigProperty(name = PropertyNames.GITHUB_BASE_URL) String githubUrl,
                                        @ConfigProperty(name = PropertyNames.GITHUB_TIMEOUT_UNIT) TimeUnit timeoutUnit,
                                        @ConfigProperty(name = PropertyNames.GITHUB_TIMEOUT_VALUE) long timeoutValue) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutValue, timeoutUnit)
                .readTimeout(timeoutValue, timeoutUnit)
                .writeTimeout(timeoutValue, timeoutUnit)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(githubUrl)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(GithubClient.class);
    }

    @Produces
    @ApplicationScoped
    public SecretManager googleSecretManager(@ConfigProperty(name = PropertyNames.PROJECT_ID) String projectId) {
        return new GoogleSecretManager(projectId);
    }

    @Produces
    @ApplicationScoped
    public EncryptedStorage datastoreEncryptedStorage(
            Datastore datastore, SecretManager secretManager,
            @ConfigProperty(name = PropertyNames.GITHUB_ENCRYPTION_KEY_SECRET) String keySecret)
            throws GeneralSecurityException {
        try {
            return new DatastoreEncryptedStorage(datastore, secretManager.getSecret(keySecret));
        } catch (NotFoundException ignore) {
            DatastoreEncryptedStorage storage = new DatastoreEncryptedStorage(datastore);
            String key = storage.getEncryptionKey();
            secretManager.saveSecret(keySecret, key);
            return storage;
        }
    }

    @Produces
    @ApplicationScoped
    public GithubCredentials githubCredentials(SecretManager secretManager,
                                               @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_ID_SECRET) String idSecret,
                                               @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_TOKEN_SECRET) String tokenSecret) {
        List<String> secrets = secretManager.getSecrets(idSecret, tokenSecret);
        return new GithubCredentials(secrets.get(0), secrets.get(1));
    }
}
