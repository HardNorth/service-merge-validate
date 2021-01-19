package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.service.GithubOAuthService;
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
                                                   @ConfigProperty(name = "net.hardnorth.application.name") String applicationName,
                                                   @ConfigProperty(name = "net.hardnorth.github.merge.url") String serviceUrl,
                                                   @ConfigProperty(name = "net.hardnorth.github.oauth.url") String githubOAuthUrl,
                                                   @ConfigProperty(name = "net.hardnorth.github.oauth.client.id") String clientId) {
        GithubOAuthService service = new GithubOAuthService(datastore, applicationName, serviceUrl, clientId);
        if (isNotBlank(githubOAuthUrl)) {
            service.setGithubOAuthUrl(githubOAuthUrl);
        }
        return service;
    }

//	@Bean
//	public MergeValidateService mergeValidateService(@Autowired Datastore datastore) {
//		return new MergeValidateService(datastore);
//	}
}
