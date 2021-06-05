package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.CommitDifference;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

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

    void createPullRequest(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                           @Nullable String source, @Nullable String dest, @Nullable String title,
                           @Nullable String body);
}
