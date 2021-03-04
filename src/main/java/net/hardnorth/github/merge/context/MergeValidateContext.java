package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.*;
import net.hardnorth.github.merge.service.impl.*;
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

@SuppressWarnings("CdiInjectionPointsInspection")
public class MergeValidateContext {

    @Produces
    @ApplicationScoped
    public OkHttpClient httpClient(@ConfigProperty(name = PropertyNames.GITHUB_TIMEOUT_UNIT) TimeUnit timeoutUnit,
                                   @ConfigProperty(name = PropertyNames.GITHUB_TIMEOUT_VALUE) long timeoutValue) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeoutValue, timeoutUnit)
                .readTimeout(timeoutValue, timeoutUnit)
                .writeTimeout(timeoutValue, timeoutUnit)
                .build();
    }

    @Produces
    @ApplicationScoped
    public GithubAuthClient githubAuthClient(@ConfigProperty(name = PropertyNames.GITHUB_BASE_URL) String githubUrl,
                                             OkHttpClient client) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(githubUrl)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(GithubAuthClient.class);
    }

    @Produces
    @ApplicationScoped
    public GithubApiClient githubApiClient(@ConfigProperty(name = PropertyNames.GITHUB_API_URL) String githubUrl,
                                           OkHttpClient client) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(githubUrl)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(GithubApiClient.class);
    }

    @Produces
    @ApplicationScoped
    public SecretManager googleSecretManager(@ConfigProperty(name = PropertyNames.PROJECT_ID) String projectId) {
        return new GoogleSecretManager(projectId);
    }

    @Produces
    @ApplicationScoped
    public GithubCredentials githubCredentials(SecretManager secretManager,
                                               @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_ID_SECRET) String idSecret,
                                               @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_TOKEN_SECRET) String tokenSecret) {
        List<String> secrets = secretManager.getSecrets(idSecret, tokenSecret);
        return new GithubCredentials(secrets.get(0), secrets.get(1));
    }

    @Produces
    @ApplicationScoped
    public Charset applicationCharset(@ConfigProperty(name = PropertyNames.CHARSET) String charsetName) {
        return new Charset(java.nio.charset.Charset.forName(charsetName));
    }

    @Produces
    @ApplicationScoped
    public Github githubService(OkHttpClient httpClient, GithubAuthClient authClient, GithubApiClient apiClient,
                                GithubCredentials githubCredentials, Charset currentCharset,
                                @ConfigProperty(name = PropertyNames.GITHUB_FILE_SIZE_LIMIT) long sizeLimit) {
        return new GithubService(httpClient, authClient, apiClient, githubCredentials, sizeLimit, currentCharset);
    }

    @Produces
    @ApplicationScoped
    public Datastore datastoreService(@ConfigProperty(name = PropertyNames.APPLICATION_NAME) String applicationName) {
        return DatastoreOptions.newBuilder().setNamespace(applicationName).build().getService();
    }

    @Produces
    @ApplicationScoped
    public GithubOAuthService authorizationService(Datastore datastore, Github githubApi, EncryptionService encryptionService,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_URL) String serviceUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_AUTHORIZE_URL) String githubOAuthUrl,
                                                   GithubCredentials credentials, Charset charset) {
        GithubOAuthService service = new GithubOAuthService(datastore, githubApi, encryptionService, serviceUrl, credentials, charset);
        if (isNotBlank(githubOAuthUrl)) {
            service.setGithubOAuthUrl(githubOAuthUrl);
        }
        return service;
    }

    @Produces
    @ApplicationScoped
    public MergeValidate mergeValidateService(Github client, Charset charset,
                                              @ConfigProperty(name = PropertyNames.APPLICATION_NAME) String applicationName) {
        return new MergeValidateService(client, "." + applicationName, charset);
    }

    @Produces
    @ApplicationScoped
    public EncryptionService tinkEncryptionService(SecretManager secretManager,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_ENCRYPTION_KEY_SECRET) String keyName)
            throws GeneralSecurityException {
        return new TinkEncryptionService(secretManager, keyName);
    }
}
