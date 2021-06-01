package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.CommitDifference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Github {
    @Nonnull
    String authenticateApplication();

    @Nonnull
    byte[] getFileContent(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                          @Nullable String branch, @Nonnull String filePath);

    @Nonnull
    String getLatestCommit(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                           @Nullable String branch);

    @Nonnull
    CommitDifference listChanges(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                                 @Nullable String source, @Nullable String dest);

    void merge(@Nullable String authHeader, @Nullable String user, @Nullable String repo, @Nullable String source,
               @Nullable String dest, @Nullable String message);
}
