package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.Datastore;

public class AuthorizationService {

	private final Datastore datastore;

	public AuthorizationService(Datastore datastoreService) {
		datastore = datastoreService;
	}

	public String getRepository(String authUuid) {
		return null;
	}
}
