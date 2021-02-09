package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;

public interface OAuthService {

    @Nonnull
    String authenticate(String authToken);

    @Nonnull
    String createIntegration();

    @Nonnull
    String authorize(@Nonnull String authToken, @Nonnull String code, @Nonnull String state);
}
