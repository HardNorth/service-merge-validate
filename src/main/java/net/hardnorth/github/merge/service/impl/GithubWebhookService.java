package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import net.hardnorth.github.merge.model.hook.*;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.GithubWebhook;
import net.hardnorth.github.merge.service.JWT;
import net.hardnorth.github.merge.service.MergeValidate;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import java.util.Date;

public class GithubWebhookService implements GithubWebhook {
    private static final Logger LOGGER = Logger.getLogger(GithubWebhookService.class);

    private static final String ZEROES = "0000000000000000000000000000000000000000";
    private static final String BRANCH_NAME_SEPARATOR = "-";
    private static final String TOKENS_KIND = "tokens";
    private static final String PULL_REQUESTS_KIND = "pull_requests";
    private static final String TOKEN = "token";
    private static final String INSTALLATION_ID = "installation_id";
    private static final String EXPIRE_DATE = "expire_date";
    private static final String PULL_NUMBER = "pull_number";
    private static final String TIMESTAMP = "timestamp";
    private static final String OWNER = "owner";
    private static final String REPOSITORY = "repo";
    private static final String SOURCE_BRANCH = "source_branch";
    private static final String TARGET_BRANCH = "target_branch";
    private static final String BEARER = "Bearer ";

    private final String appName;
    private final Github github;
    private final MergeValidate merge;
    private final JWT jwt;
    private final Datastore datastore;
    private final KeyFactory tokenKeyFactory;
    private final KeyFactory pullsKeyFactory;

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
    }

    private void createInstallation(InstallationRequest installationRequest) {
        Installation installation = installationRequest.getInstallation();
        Account account = installation.getAccount();
        LOGGER.infof("New installation '%d' by '%s' '%s' with user ID '%d'", installation.getId(),
                account.getType(), account.getLogin(), account.getId());
    }

    private void deleteInstallation(InstallationRequest installationRequest) {
        Installation installation = installationRequest.getInstallation();
        Account account = installation.getAccount();
        LOGGER.infof("Installation '%d' deleted by '%s' '%s' with user ID '%d'", installation.getId(),
                account.getType(), account.getLogin(), account.getId());
    }

    @Override
    public void processInstallation(InstallationRequest installationRequest) {
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
    public void processPush(PushRequest pushRequest) {
        if (!pushRequest.isCreated() || !ZEROES.equals(pushRequest.getBefore())) {
            return; // Work only with new branches
        }

        String workBranch = pushRequest.getRef().substring(pushRequest.getRef().lastIndexOf('/') + 1);
        if(!workBranch.startsWith(appName + BRANCH_NAME_SEPARATOR)) {
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

        String owner = pushRequest.getRepository().getOwner().getName();
        String repository = pushRequest.getRepository().getName();

        merge.validate(BEARER + token, owner, repository, workBranch, targetBranch);

        int pullNumber = github.createPullRequest(BEARER + token, owner, repository, workBranch, targetBranch,
                "Merge " + workBranch + " to " + targetBranch, null);

        Entity entity = Entity
                .newBuilder(datastore.allocateId(pullsKeyFactory.newKey()))
                .set(PULL_NUMBER, pullNumber)
                .set(TIMESTAMP, Timestamp.now())
                .set(OWNER, owner)
                .set(REPOSITORY, repository)
                .set(SOURCE_BRANCH, workBranch)
                .set(TARGET_BRANCH, targetBranch)
                .build();
        datastore.put(entity);
    }

    @Override
    public void processPull(PullRequest pullRequest) {
        LOGGER.infof("Pull request action '%s' on pull request '%d' in repository '%s' of user '%s'",
                pullRequest.getAction(), pullRequest.getNumber(), pullRequest.getRepository().getName(),
                pullRequest.getRepository().getOwner().getName());
    }

    @Override
    public void processCheckRun(CheckRunRequest checkRunRequest) {
        LOGGER.infof("Check run Action '%s' in repository '%s' of user '%s'", checkRunRequest.getAction(),
                checkRunRequest.getRepository().getName(), checkRunRequest.getRepository().getOwner().getName());
    }
}
