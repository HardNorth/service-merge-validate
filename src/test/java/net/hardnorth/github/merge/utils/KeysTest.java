package net.hardnorth.github.merge.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class KeysTest {

    @Test
    public void verify_uuid_bytes_get() {
        UUID token = UUID.randomUUID();
        byte[] tokenBytes = Keys.getBytes(token);

        String tokenBytesResult = Hex.encodeHexString(tokenBytes);
        assertThat(tokenBytesResult, equalTo(token.toString().replace("-", "")));
    }

    @Test
    public void verify_token_encode_decode() {
        UUID token = UUID.randomUUID();
        byte[] tokenBytes = Keys.getBytes(token);

        long key = new Random().nextLong();
        byte[] keyBytes = BigInteger.valueOf(key).toByteArray();

        KeyType type = KeyType.LONG;

        String encodedToken = Keys.encodeAuthToken(type, keyBytes, tokenBytes);
        assertThat(encodedToken.length(), greaterThan(0));

        Triple<KeyType, byte[], byte[]> decodedToken = Keys.decodeAuthToken(encodedToken);

        assertThat(decodedToken.getLeft(), sameInstance(type));
        assertThat(new BigInteger(decodedToken.getMiddle()).longValue(), equalTo(key));

        String tokenBytesResult = Hex.encodeHexString(decodedToken.getRight());
        assertThat(tokenBytesResult, equalTo(token.toString().replace("-", "")));
    }

    private static String[] invalidTokens() {
        return new String[]{"333", null, "AAcSSY81", "AAcSSY81gAAA", "AAcSSY81gAAAj", "AAcSSY81gAAAj%", "AAcSSY81gAAAj&",
        "https://github.com/login/oauth/authorize?redirect_uri=http%3A%2F%2Flocalhost%3A8889%2Fintegration%2Fresult%2FAAcWat81gAAAeEg2ziDZSPK6Ff5jmxKN7A&client_id=test-client-id&scope=repo+user%3Aemail&state=51018ada-32c5-4635-956b-bd2304dba1d4"};
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    public void verify_invalid_token_decode(String value) {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> Keys.decodeAuthToken(value));
        assertThat(exception.getMessage(), equalTo("Invalid token"));
    }

    @Test
    public void verify_minimal_token_decode() {
        String oneByteToken = "AAcSSY81gAAAjo";
        Triple<KeyType, byte[], byte[]> decodedToken = Keys.decodeAuthToken(oneByteToken);

        assertThat(decodedToken.getLeft(), sameInstance(KeyType.LONG));
        assertThat(new BigInteger(decodedToken.getMiddle()).longValue(), equalTo(5147429007523840L));
        assertThat(Hex.encodeHexString(decodedToken.getRight()), equalTo("8e"));
    }
}
