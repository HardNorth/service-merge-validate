package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.exception.NotFoundException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.*;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
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
    public GithubOAuthService authorizationService(Datastore datastore, GithubClient githubApi,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_URL) String serviceUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_AUTHORIZE_URL) String githubOAuthUrl,
                                                   GithubCredentials credentials) {
        GithubOAuthService service = new GithubOAuthService(datastore, githubApi, serviceUrl, credentials);
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
        byte[] key;
        try {
            key = Base64.getDecoder().decode(secretManager.getSecrets(keySecret).get(0));
        } catch (NotFoundException ignore) {
            try {
                key = Hex.decodeHex(UUID.randomUUID().toString().replace("-", ""));
            } catch (DecoderException e) {
                throw new RuntimeException(e); // actually should neve happen, since UUIDs use hex strings
            }

            secretManager.saveSecret(keySecret, Base64.getEncoder().encodeToString(key));
        }
        return new DatastoreEncryptedStorage(datastore, key);
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
