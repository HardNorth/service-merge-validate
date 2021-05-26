package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;

public interface GithubAppService {

    @Nonnull
    String install(@Nonnull String code, @Nonnull String state);
}
