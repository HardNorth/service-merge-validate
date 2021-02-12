package net.hardnorth.github.merge.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class Keys {

    private Keys() {

    }

    public static final RuntimeException INVALID_TOKEN = new IllegalArgumentException("Invalid token");

    @Nonnull
    public static String removeDashes(@Nonnull String input) {
        return input.replace("-", "");
    }

    @Nonnull
    public static byte[] getKeyBytes(@Nonnull Object keyValue) {
        if (keyValue instanceof Number) {
            return BigInteger.valueOf(((Number) keyValue).longValue()).toByteArray();
        } else {
            String authKeyValueStr = keyValue.toString();
            return authKeyValueStr.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Nonnull
    public static String getBare(@Nonnull UUID uuid) {
        return removeDashes(uuid.toString());
    }

    @Nonnull
    public static byte[] getBytes(@Nonnull UUID uuid) {
        try {
            return Hex.decodeHex(getBare(uuid));
        } catch (DecoderException e) {
            // UUIDs are hex strings, so this should not happen
            throw new IllegalStateException(e);
        }
    }

    @Nonnull
    public static String encodeAuthToken(@Nonnull KeyType type, @Nonnull byte[] keyBytes, @Nonnull byte[] authUuidBytes) {
        byte[] authToken = new byte[keyBytes.length + authUuidBytes.length + 2];
        authToken[0] = (byte) type.ordinal();
        authToken[1] = (byte) keyBytes.length;
        System.arraycopy(keyBytes, 0, authToken, 2, keyBytes.length);
        System.arraycopy(authUuidBytes, 0, authToken, 2 + keyBytes.length, authUuidBytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(authToken);
    }

    @Nonnull
    public static Triple<KeyType, byte[], byte[]> decodeAuthToken(@Nullable String token) {
        if (token == null) {
            throw INVALID_TOKEN;
        }
        byte[] tokenBytes;
        try {
            tokenBytes = Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException e) {
            throw INVALID_TOKEN;
        }
        if (tokenBytes[0] < 0 || tokenBytes[0] > KeyType.values().length) {
            throw INVALID_TOKEN;
        }
        KeyType type = KeyType.values()[tokenBytes[0]];
        if (tokenBytes[1] < 0 || tokenBytes[1] > tokenBytes.length - 3) {
            throw INVALID_TOKEN;
        }
        byte[] keyBytes = new byte[tokenBytes[1]];
        System.arraycopy(tokenBytes, 2, keyBytes, 0, keyBytes.length);
        byte[] authUuidBytes = new byte[tokenBytes.length - 2 - keyBytes.length];
        System.arraycopy(tokenBytes, 2 + keyBytes.length, authUuidBytes, 0, authUuidBytes.length);
        return Triple.of(type, keyBytes, authUuidBytes);
    }
}
