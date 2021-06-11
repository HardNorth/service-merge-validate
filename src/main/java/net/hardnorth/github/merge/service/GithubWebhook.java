package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.github.hook.EventCheckRun;
import net.hardnorth.github.merge.model.github.hook.EventInstallation;
import net.hardnorth.github.merge.model.github.hook.EventPullRequest;
import net.hardnorth.github.merge.model.github.hook.EventPush;

public interface GithubWebhook {
    void processInstallation(EventInstallation installationRequest);

    void processPush(EventPush pushRequest);

    void processPull(EventPullRequest pullRequest);

    void processCheckRun(EventCheckRun checkRunRequest);
}
