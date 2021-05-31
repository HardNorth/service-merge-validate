package net.hardnorth.github.merge.service.impl;

import net.hardnorth.github.merge.model.hook.InstallationRequest;
import net.hardnorth.github.merge.model.hook.PushRequest;
import net.hardnorth.github.merge.service.GithubWebhook;
import net.hardnorth.github.merge.service.MergeValidate;
import org.jboss.logging.Logger;

public class GithubWebhookService implements GithubWebhook {

    private static final Logger LOGGER = Logger.getLogger(GithubWebhookService.class);

    private final MergeValidate merge;

    public GithubWebhookService(MergeValidate mergeValidate){
        merge = mergeValidate;
    }

    @Override
    public void processInstallation(InstallationRequest installationRequest) {
        LOGGER.info("Installation processing");
    }

    @Override
    public void processPush(PushRequest pushRequest) {

    }
}
