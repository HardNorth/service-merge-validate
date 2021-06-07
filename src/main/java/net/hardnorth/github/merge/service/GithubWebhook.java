package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.hook.CheckRunRequest;
import net.hardnorth.github.merge.model.hook.InstallationRequest;
import net.hardnorth.github.merge.model.hook.PullRequest;
import net.hardnorth.github.merge.model.hook.PushRequest;

public interface GithubWebhook {
    void processInstallation(InstallationRequest installationRequest);

    void processPush(PushRequest pushRequest);

    void processPull(PullRequest pullRequest);

    void processCheckRun(CheckRunRequest checkRunRequest);
}
