package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.Datastore;

public class MergeValidateService {

    private final Datastore datastore;

    public MergeValidateService(Datastore datastoreService) {
        datastore = datastoreService;
    }

}
