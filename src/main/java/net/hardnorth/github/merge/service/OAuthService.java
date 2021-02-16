package net.hardnorth.github.merge.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface OAuthService {

    @Nonnull
    String authenticate(@Nullable String authToken);

    @Nonnull
    String createIntegration();

    @Nonnull
    String authorize(@Nonnull String authToken, @Nonnull String code, @Nonnull String state);
}
