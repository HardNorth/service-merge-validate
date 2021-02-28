package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.Change;
import net.hardnorth.github.merge.model.CommitDifference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface Github {
    @Nonnull
    String loginApplication(@Nullable String code, @Nullable String state);

    @Nonnull
    byte[] getFileContent(@Nullable String authHeader, @Nullable String repo, @Nullable String branch,
                          @Nonnull String filePath);

    @Nonnull
    String getLatestCommit(@Nullable String authHeader, @Nullable String repo, @Nullable String branch);

    @Nonnull
    CommitDifference listChanges(@Nullable String authHeader, @Nullable String repo, @Nullable String source,
                                 @Nullable String dest);
}
