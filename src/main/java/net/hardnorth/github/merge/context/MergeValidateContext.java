package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.service.GithubClientApi;
import net.hardnorth.github.merge.service.GithubOAuthService;
import net.hardnorth.github.merge.service.MergeValidateService;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class MergeValidateContext {
    @Produces
    @ApplicationScoped
    public Datastore datastoreService() {
        return DatastoreOptions.getDefaultInstance().getService();
    }

    @Produces
    @ApplicationScoped
    public GithubOAuthService authorizationService(Datastore datastore, GithubClientApi githubApi,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_NAME) String applicationName,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_URL) String serviceUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_AUTHORIZE_URL) String githubOAuthUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_ID) String clientId,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_SECRET) String clientSecret) {
        GithubOAuthService service = new GithubOAuthService(datastore, githubApi, applicationName, serviceUrl, clientId, clientSecret);
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
    public GithubClientApi githubClientApi(@ConfigProperty(name = PropertyNames.GITHUB_BASE_URL) String githubUrl,
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
        return retrofit.create(GithubClientApi.class);
    }
}
