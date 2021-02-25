package net.hardnorth.github.merge.service;

import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public interface Github {
    @Nonnull
    Pair<String, String> loginApplication(String code, String state);
    @Nonnull
    byte[] getFileContent(String authHeader, String repo, String branch, String filePath);
}
