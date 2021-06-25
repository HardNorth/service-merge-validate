package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.CommitDifference;
import net.hardnorth.github.merge.model.github.repo.BranchProtection;
import net.hardnorth.github.merge.model.github.repo.PullRequest;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public interface Github {

    @Nonnull
    Pair<String, Date> authenticateInstallation(@Nullable String authHeader, long installationId);

    @Nonnull
    byte[] getFileContent(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                          @Nullable String branch, @Nonnull String filePath);

    @Nonnull
    String getLatestCommit(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                           @Nullable String branch);

    @Nonnull
    CommitDifference listChanges(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                                 @Nullable String source, @Nullable String dest);

    void merge(@Nullable String authHeader, @Nullable String owner, @Nullable String repo, @Nullable String source,
               @Nullable String dest, @Nullable String message);

    int createPullRequest(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                          @Nullable String source, @Nullable String dest, @Nullable String title,
                          @Nullable String body);

    void createReview(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                      int pullNumber, @Nullable String event, @Nullable String body);

    void mergePullRequest(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                          int pullNumber, @Nullable String commitTitle, @Nullable String commitMessage,
                          @Nullable String mergeMethod);

    BranchProtection getBranchProtection(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                                         @Nullable String branch);

    List<PullRequest> getOpenedPullRequests(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                                            @Nullable String branch);

    PullRequest getPullRequest(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                               int pullNumber);
}
