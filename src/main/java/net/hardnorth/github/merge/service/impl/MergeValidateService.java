package net.hardnorth.github.merge.service.impl;

import net.hardnorth.github.merge.service.GithubApiClient;
import net.hardnorth.github.merge.service.MergeValidate;

public class MergeValidateService implements MergeValidate {

    private final GithubApiClient client;

    public MergeValidateService(GithubApiClient githubClient) {
        client = githubClient;
    }

    @Override
    public void merge(String authToken, String repo, String from, String to) {

    }
}
