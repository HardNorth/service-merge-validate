package net.hardnorth.github.merge.context;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.service.AuthorizationService;
import net.hardnorth.github.merge.service.MergeValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:merge-validate.properties")
public class MergeValidateContext {
	@Bean
	public Datastore datastoreService() {
		return DatastoreOptions.getDefaultInstance().getService();
	}

	@Bean
	public AuthorizationService authorizationService(@Autowired Datastore datastore) {
		return new AuthorizationService(datastore);
	}

	@Bean
	public MergeValidateService mergeValidateService(@Autowired Datastore datastore) {
		return new MergeValidateService(datastore);
	}
}
