package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.*;
import net.hardnorth.github.merge.service.impl.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("CdiInjectionPointsInspection")
public class MergeValidateContext {

    @Produces
    @ApplicationScoped
    public OkHttpClient httpClient(@ConfigProperty(name = PropertyNames.GITHUB_TIMEOUT_UNIT) TimeUnit timeoutUnit,
                                   @ConfigProperty(name = PropertyNames.GITHUB_TIMEOUT_VALUE) long timeoutValue,
                                   @ConfigProperty(name = PropertyNames.GITHUB_LOG) boolean log) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(timeoutValue, timeoutUnit)
                .readTimeout(timeoutValue, timeoutUnit)
                .writeTimeout(timeoutValue, timeoutUnit);

        if (log) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(interceptor);
        }

        return builder.build();
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
    public Charset applicationCharset(@ConfigProperty(name = PropertyNames.CHARSET) String charsetName) {
        return new Charset(java.nio.charset.Charset.forName(charsetName));
    }

    @Produces
    @ApplicationScoped
    public SecretManager googleSecretManager(@ConfigProperty(name = PropertyNames.PROJECT_ID) String projectId,
                                             Charset charset) {
        return new GoogleSecretManager(projectId, charset);
    }

    @Produces
    @ApplicationScoped
    public JWT applicationKey(SecretManager secretManager,
                              @ConfigProperty(name = PropertyNames.GITHUB_APP_ID) String applicationId,
                              @ConfigProperty(name = PropertyNames.GITHUB_RSA_KEY_SECRET) String keyName)
            throws IOException {
        byte[] rsaKeyBytes = secretManager.getRawSecret(keyName);
        return new JwtService(applicationId, rsaKeyBytes);
    }

    @Produces
    @ApplicationScoped
    public Github githubService(GithubApiClient apiClient, Charset currentCharset,

                                @ConfigProperty(name = PropertyNames.GITHUB_FILE_SIZE_LIMIT) long sizeLimit) {
        return new GithubService(apiClient, sizeLimit, currentCharset);
    }

    @Produces
    @ApplicationScoped
    public Datastore datastoreService(@ConfigProperty(name = PropertyNames.APPLICATION_NAME) String applicationName) {
        return DatastoreOptions.newBuilder().setNamespace(applicationName).build().getService();
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
                                                   @ConfigProperty(name = PropertyNames.ENCRYPTION_KEY_SECRET) String keyName)
            throws GeneralSecurityException {
        return new TinkEncryptionService(secretManager, keyName);
    }

    @Produces
    @ApplicationScoped
    public GithubWebhook githubWebhookService(@ConfigProperty(name = PropertyNames.APPLICATION_NAME) String appName,
                                              Github github, MergeValidate mergeValidate, JWT jwt, Datastore datastore) {
        return new GithubWebhookService(appName, github, mergeValidate, jwt, datastore);
    }
}
