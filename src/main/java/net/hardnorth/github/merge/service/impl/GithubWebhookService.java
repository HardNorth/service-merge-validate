package net.hardnorth.github.merge.service.impl;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.KeyFactory;
import net.hardnorth.github.merge.model.hook.InstallationRequest;
import net.hardnorth.github.merge.model.hook.PushRequest;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.GithubWebhook;
import net.hardnorth.github.merge.service.JWT;
import net.hardnorth.github.merge.service.MergeValidate;
import org.jboss.logging.Logger;

public class GithubWebhookService implements GithubWebhook {

    private static final String INSTALLATIONS_KIND = "installations";

    private static final Logger LOGGER = Logger.getLogger(GithubWebhookService.class);

    private final Github github;
    private final MergeValidate merge;
    private final JWT jwt;
    private final Datastore datastore;
    private final KeyFactory installationKeyFactory;

    public GithubWebhookService(Github githubService, MergeValidate mergeValidate, JWT jwtService, Datastore datastoreService){
        github = githubService;
        merge = mergeValidate;
        jwt = jwtService;
        datastore = datastoreService;
        installationKeyFactory = datastore.newKeyFactory().setKind(INSTALLATIONS_KIND);
    }

    @Override
    public void processInstallation(InstallationRequest installationRequest) {
        LOGGER.info("Installation processing");
    }

    @Override
    public void processPush(PushRequest pushRequest) {

    }
}
