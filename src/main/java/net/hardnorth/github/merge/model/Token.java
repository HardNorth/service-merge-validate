package net.hardnorth.github.merge.model;

import net.hardnorth.github.merge.utils.Keys;
import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.annotation.Nonnull;
import java.util.UUID;

public class Token {
    private final byte[] value;
    private final String hash;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public Token(@Nonnull final UUID source) {
        this(Keys.getBytes(source));
    }

    public Token(@Nonnull final byte[] token) {
        this(token, BCrypt.hashpw(token, BCrypt.gensalt(12)));
    }

    public Token(@Nonnull final byte[] token, @Nonnull final String hashString) {
        value = token;
        hash = hashString;
    }

    public byte[] getValue() {
        return value;
    }

    public String getHash() {
        return hash;
    }
}
