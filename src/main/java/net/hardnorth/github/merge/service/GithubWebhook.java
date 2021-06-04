package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.hook.InstallationRequest;
import net.hardnorth.github.merge.model.hook.PushRequest;

public interface GithubWebhook {
    void processInstallation(InstallationRequest installationRequest);

    void processPush(PushRequest pushRequest);
}
