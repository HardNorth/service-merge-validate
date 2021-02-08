package net.hardnorth.github.merge.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Triple;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class Keys {

    public static String removeDashes(String input) {
        return input.replace("-", "");
    }

    public static byte[] getKeyBytes(Object keyValue) {
        if(keyValue instanceof Number) {
            return BigInteger.valueOf(((Number) keyValue).longValue()).toByteArray();
        } else {
            String authKeyValueStr = keyValue.toString();
            return authKeyValueStr.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static String getBare(UUID uuid) {
        return removeDashes(uuid.toString());
    }

    public static byte[] getBytes(UUID uuid) {
        try {
        return Hex.decodeHex(getBare(uuid));
        } catch (DecoderException e) {
            // UUIDs are hex strings, so this should not happen
            throw new IllegalStateException(e);
        }
    }

    public static String getAuthToken(KeyType type, byte[] keyBytes, byte[] authUuidBytes) {
        byte[] authToken = new byte[keyBytes.length + authUuidBytes.length + 2];
        authToken[0] = (byte) type.ordinal();
        authToken[1] = (byte) keyBytes.length;
        System.arraycopy(keyBytes, 0, authToken, 2, keyBytes.length);
        System.arraycopy(authUuidBytes, 0, authToken, 2 + keyBytes.length, authUuidBytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(authToken);
    }

    public static Triple<KeyType, byte[], byte[]> decodeAuthToken(String token) {
        byte[] tokenBytes = Base64.getUrlDecoder().decode(token);
        KeyType type = KeyType.values()[tokenBytes[0]];
        byte[] keyBytes = new byte[tokenBytes[1]];
        System.arraycopy(tokenBytes, 2, keyBytes, 0, keyBytes.length);
        byte[] authUuidBytes = new byte[tokenBytes.length - 2 - tokenBytes[1]];
        System.arraycopy(tokenBytes, 2 + tokenBytes.length, authUuidBytes, 0, authUuidBytes.length);
        return Triple.of(type, keyBytes, authUuidBytes);
    }
}
