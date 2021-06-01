package net.hardnorth.github.merge.context;

import com.auth0.jwt.algorithms.Algorithm;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.model.JwtAlgorithm;
import net.hardnorth.github.merge.service.*;
import net.hardnorth.github.merge.service.impl.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
    public GithubCredentials githubCredentials(SecretManager secretManager,
                                               @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_ID_SECRET) String idSecret,
                                               @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_TOKEN_SECRET) String tokenSecret) {
        List<String> secrets = secretManager.getSecrets(idSecret, tokenSecret);
        return new GithubCredentials(secrets.get(0), secrets.get(1));
    }

    @Produces
    @ApplicationScoped
    public JwtAlgorithm applicationKey(SecretManager secretManager,
                                       @ConfigProperty(name = PropertyNames.GITHUB_RSA_KEY_SECRET) String keyName)
            throws IOException {
        byte[] rsaKeyBytes = secretManager.getRawSecret(keyName);
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(rsaKeyBytes)));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair;
        if (object instanceof PEMKeyPair) {
            keyPair = converter.getKeyPair((PEMKeyPair) object);
        } else {
            throw new IllegalArgumentException("Invalid application key format");
        }
        return new JwtAlgorithm(Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate()));
    }

    @Produces
    @ApplicationScoped
    public Github githubService(OkHttpClient httpClient, JwtAlgorithm key, GithubApiClient apiClient,
                                GithubCredentials githubCredentials, Charset currentCharset,
                                @ConfigProperty(name = PropertyNames.GITHUB_APP_ID) String applicationId,
                                @ConfigProperty(name = PropertyNames.GITHUB_FILE_SIZE_LIMIT) long sizeLimit) {
        return new GithubService(applicationId, httpClient, key, apiClient, githubCredentials, sizeLimit, currentCharset);
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
    public GithubWebhook githubWebhookService(MergeValidate mergeValidate) {
        return new GithubWebhookService(mergeValidate);
    }
}
