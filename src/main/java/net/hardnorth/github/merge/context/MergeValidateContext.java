package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.service.GithubOAuthService;
import net.hardnorth.github.merge.service.MergeValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Configuration
@PropertySource("classpath:merge-validate.properties")
public class MergeValidateContext {
	@Bean
	public Datastore datastoreService() {
		return DatastoreOptions.getDefaultInstance().getService();
	}

	@Bean
	public GithubOAuthService authorizationService(@Autowired Datastore datastore,
			@Value("${net.hardnorth.github.merge.url}") String serviceUrl, @Value("${spring.cloud.appId}") String applicationName,
			@Value("${net.hardnorth.github.oauth.client.id}") String clientId,
			@Value("${net.hardnorth.github.oauth.url:}") String githubOAuthUrl) {
		GithubOAuthService service = new GithubOAuthService(datastore, serviceUrl, applicationName, clientId);
		if (isNotBlank(githubOAuthUrl)) {
			service.setGithubOAuthUrl(githubOAuthUrl);
		}
		return service;
	}

	@Bean
	public MergeValidateService mergeValidateService(@Autowired Datastore datastore) {
		return new MergeValidateService(datastore);
	}
}
