package net.hardnorth.github.merge.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import net.hardnorth.github.merge.service.impl.JwtService;
import net.hardnorth.github.merge.utils.IoUtils;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class JwtServiceTest {

    public static final String GITHUB_APP_ID = "72458";
    private static final byte[] KEY = IoUtils
            .readInputStreamToBytes(GithubServiceTest.class.getClassLoader()
                    .getResourceAsStream("encryption/merge-validate-test-key.pem"));
    private static Algorithm ALGORITHM;

    public JWT jwt;

    @BeforeAll
    public static void setup() throws IOException {
        if (KEY == null) {
            throw new IllegalStateException("Unable to find test RSA key");
        }
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(KEY)));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair;
        if (object instanceof PEMKeyPair) {
            keyPair = converter.getKeyPair((PEMKeyPair) object);
        } else {
            throw new IllegalArgumentException("Invalid application key format");
        }
        ALGORITHM = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());

    }

    @BeforeEach
    public void init() throws IOException {
        if (KEY == null) {
            throw new IllegalStateException("Unable to find test RSA key");
        }
        jwt = new JwtService(GITHUB_APP_ID, KEY);
    }

    @Test
    public void verify_authentication_token() {
        String token = jwt.get();
        assertThat(token, notNullValue());
        JWTVerifier verifier = com.auth0.jwt.JWT.require(ALGORITHM).withIssuer(GITHUB_APP_ID).build();
        assertThat(verifier.verify(token), notNullValue());
    }
}
