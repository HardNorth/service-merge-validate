package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.service.GithubOAuthService;
import net.hardnorth.github.merge.service.MergeValidateService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class MergeValidateContext {
    @Produces
    @ApplicationScoped
    public Datastore datastoreService() {
        return DatastoreOptions.getDefaultInstance().getService();
    }

    @Produces
    @ApplicationScoped
    public GithubOAuthService authorizationService(Datastore datastore,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_NAME) String applicationName,
                                                   @ConfigProperty(name = PropertyNames.APPLICATION_URL) String serviceUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_URL) String githubOAuthUrl,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_ID) String clientId,
                                                   @ConfigProperty(name = PropertyNames.GITHUB_CLIENT_SECRET) String clientSecret) {
        GithubOAuthService service = new GithubOAuthService(datastore, applicationName, serviceUrl, clientId, clientSecret);
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
}
