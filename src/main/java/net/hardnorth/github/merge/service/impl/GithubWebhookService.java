package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import net.hardnorth.github.merge.model.github.hook.*;
import net.hardnorth.github.merge.model.github.repo.BranchProtection;
import net.hardnorth.github.merge.model.github.repo.PullRequest;
import net.hardnorth.github.merge.model.github.repo.RequiredPullRequestReviews;
import net.hardnorth.github.merge.model.github.repo.RequiredStatusChecks;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.GithubWebhook;
import net.hardnorth.github.merge.service.JWT;
import net.hardnorth.github.merge.service.MergeValidate;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

public class GithubWebhookService implements GithubWebhook {
    private static final Logger LOGGER = Logger.getLogger(GithubWebhookService.class);

    private static final String ZEROES = "0000000000000000000000000000000000000000";
    private static final String BRANCH_NAME_SEPARATOR = "-";
    private static final String TOKENS_KIND = "tokens";
    private static final String PULL_REQUESTS_KIND = "pull_requests";
    private static final String REQUIRED_CHECKS_KIND = "required_checks";
    private static final String TOKEN = "token";
    private static final String INSTALLATION_ID = "installation_id";
    private static final String EXPIRE_DATE = "expire_date";
    private static final String PULL_NUMBER = "pull_number";
    private static final String PULL_ID = "pull_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String OWNER = "owner";
    private static final String REPOSITORY = "repo";
    private static final String SOURCE_BRANCH = "source_branch";
    private static final String TARGET_BRANCH = "target_branch";
    private static final String CHECK_NAME = "check_name";
    private static final String CHECK_PASSED = "check_passed";
    private static final String BEARER = "Bearer ";
    private static final String APPROVE_EVENT = "APPROVE";

    private final String appName;
    private final Github github;
    private final MergeValidate merge;
    private final JWT jwt;
    private final Datastore datastore;
    private final KeyFactory tokenKeyFactory;
    private final KeyFactory pullsKeyFactory;
    private final KeyFactory checksKeyFactory;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubWebhookService(String applicationName, Github githubService, MergeValidate mergeValidate,
                                JWT jwtService, Datastore datastoreService) {
        appName = applicationName;
        github = githubService;
        merge = mergeValidate;
        jwt = jwtService;
        datastore = datastoreService;
        tokenKeyFactory = datastore.newKeyFactory().setKind(TOKENS_KIND);
        pullsKeyFactory = datastore.newKeyFactory().setKind(PULL_REQUESTS_KIND);
        checksKeyFactory = datastore.newKeyFactory().setKind(REQUIRED_CHECKS_KIND);
    }

    private void createInstallation(EventInstallation installationRequest) {
        Installation installation = installationRequest.getInstallation();
        Account account = installation.getAccount();
        LOGGER.infof("New installation '%d' by '%s' '%s' with user ID '%d'", installation.getId(),
                account.getType(), account.getLogin(), account.getId());
    }

    private void deleteInstallation(EventInstallation installationRequest) {
        Installation installation = installationRequest.getInstallation();
        Account account = installation.getAccount();
        LOGGER.infof("Installation '%d' deleted by '%s' '%s' with user ID '%d'", installation.getId(),
                account.getType(), account.getLogin(), account.getId());
    }

    @Override
    public void processInstallation(EventInstallation installationRequest) {
        final String action = installationRequest.getAction();
        switch (action) {
            case "created":
                createInstallation(installationRequest);
                break;
            case "deleted":
                deleteInstallation(installationRequest);
                break;
            default:
                throw new IllegalArgumentException("Unknown installation action: " + action);
        }
    }

    @Override
    public void processPush(EventPush pushRequest) {
        if (!pushRequest.isCreated() || !ZEROES.equals(pushRequest.getBefore())) {
            return; // Work only with new branches
        }

        String workBranch = pushRequest.getRef().substring(pushRequest.getRef().lastIndexOf('/') + 1);

        if (!workBranch.startsWith(appName + BRANCH_NAME_SEPARATOR)) {
            return; // Work only with branches which match pattern: application_name-target_branch
        }

        String targetBranch = workBranch.substring(appName.length() + BRANCH_NAME_SEPARATOR.length());

        Long installationId = pushRequest.getInstallation().getId();
        if (installationId == null) {
            throw new IllegalArgumentException("Invalid request: no installation ID");
        }
        EntityQuery query = Query
                .newEntityQueryBuilder()
                .setKind(TOKENS_KIND)
                .setFilter(
                        StructuredQuery.CompositeFilter.and(
                                StructuredQuery.PropertyFilter.eq(INSTALLATION_ID, installationId),
                                StructuredQuery.PropertyFilter.gt(EXPIRE_DATE, Timestamp.now())
                        ))
                .setLimit(1)
                .build();

        QueryResults<Entity> tokenResult = datastore.run(query);
        String token;
        if (!tokenResult.hasNext()) {
            Pair<String, Date> tokenResponse =
                    github.authenticateInstallation(BEARER + jwt.get(), installationId);
            Entity entity = Entity
                    .newBuilder(datastore.allocateId(tokenKeyFactory.newKey()))
                    .set(TOKEN, tokenResponse.getKey())
                    .set(INSTALLATION_ID, installationId)
                    .set(EXPIRE_DATE, Timestamp.of(tokenResponse.getValue()))
                    .build();
            datastore.put(entity);
            token = tokenResponse.getKey();
        } else {
            token = tokenResult.next().getString(TOKEN);
        }

        String auth = BEARER + token;
        String owner = pushRequest.getRepository().getOwner().getName();
        String repository = pushRequest.getRepository().getName();

        merge.validate(auth, owner, repository, workBranch, targetBranch);

        Key prKey = datastore.allocateId(pullsKeyFactory.newKey());
        BranchProtection protection = github.getBranchProtection(auth, owner, repository, targetBranch);
        Boolean hasChecks =
                ofNullable(protection.getRequiredStatusChecks())
                        .map(RequiredStatusChecks::getContexts)
                        .map(c -> !c.isEmpty())
                        .orElse(false);
        boolean needReview =
                ofNullable(protection.getRequiredPullRequestReviews()).isPresent();
        Boolean requireCodeOwnerReview =
                ofNullable(protection.getRequiredPullRequestReviews())
                        .map(RequiredPullRequestReviews::getRequireCodeOwnerReviews)
                        .orElse(false);
        String requestTitle = "Merge " + workBranch + " to " + targetBranch;

        if (!hasChecks && !needReview) {
            // No checks or review set, just merge the branch if it valid
            github.merge(auth, owner, repository, workBranch, targetBranch, requestTitle);
            return;
        }

        if (hasChecks) {
            List<String> checks = ofNullable(protection.getRequiredStatusChecks())
                    .map(RequiredStatusChecks::getContexts).orElse(Collections.emptyList());
            Entity[] checkEntities = checks.stream().map(c -> {
                Key checkKey = datastore.allocateId(checksKeyFactory.newKey());
                return Entity
                        .newBuilder(checkKey)
                        .set(PULL_ID, prKey)
                        .set(CHECK_NAME, c)
                        .set(CHECK_PASSED, false)
                        .set(REPOSITORY, repository)
                        .set(INSTALLATION_ID, installationId)
                        .set(SOURCE_BRANCH, workBranch)
                        .build();
            }).toArray(Entity[]::new);
            datastore.put(checkEntities);
        }

        if (needReview) {
            List<PullRequest> pullRequests = github.getOpenedPullRequests(auth, owner, repository, workBranch);
            int pullNumber;
            if (!pullRequests.isEmpty()) {
                pullNumber = pullRequests.get(0).getNumber();
            } else {
                pullNumber = github.createPullRequest(auth, owner, repository, workBranch, targetBranch, requestTitle,
                        null);
            }

            Entity entity = Entity
                    .newBuilder(prKey)
                    .set(PULL_NUMBER, pullNumber)
                    .set(TIMESTAMP, Timestamp.now())
                    .set(OWNER, owner)
                    .set(REPOSITORY, repository)
                    .set(SOURCE_BRANCH, workBranch)
                    .set(TARGET_BRANCH, targetBranch)
                    .build();
            datastore.put(entity);

            if (!hasChecks) {
                github.createReview(auth, owner, repository, pullNumber, APPROVE_EVENT, null);
                github.mergePullRequest(auth, owner, repository, pullNumber, requestTitle, null, null);
            }
        }
    }

    @Override
    public void processPull(EventPullRequest pullRequest) {
        LOGGER.infof("Pull request action '%s' on pull request '%d' in repository '%s' of user '%s'",
                pullRequest.getAction(), pullRequest.getNumber(), pullRequest.getRepository().getName(),
                pullRequest.getRepository().getOwner().getName());
    }

    @Override
    public void processCheckRun(EventCheckRun checkRunRequest) {
        LOGGER.infof("Check run Action '%s' in repository '%s' of user '%s'", checkRunRequest.getAction(),
                checkRunRequest.getRepository().getName(), checkRunRequest.getRepository().getOwner().getName());

    }
}
