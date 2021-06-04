package net.hardnorth.github.merge.service.impl;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import net.hardnorth.github.merge.service.JWT;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

public class JwtService implements JWT {

    private final String issuer;
    private final Algorithm algorithm;

    public JwtService(@Nonnull final String tokenIssuer, @Nonnull byte[] pkcs1RsaCertificate) throws IOException {
        issuer = tokenIssuer;
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pkcs1RsaCertificate)));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair;
        if (object instanceof PEMKeyPair) {
            keyPair = converter.getKeyPair((PEMKeyPair) object);
        } else {
            throw new IllegalArgumentException("Invalid application key format");
        }
        algorithm = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
    }

    @Nonnull
    @Override
    public String get() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
        calendar.add(Calendar.MINUTE, -1);
        JWTCreator.Builder jwtBuilder = com.auth0.jwt.JWT.create().withIssuer(issuer).withIssuedAt(calendar.getTime());
        calendar.add(Calendar.MINUTE, 10);
        return jwtBuilder.withExpiresAt(calendar.getTime()).sign(algorithm);
    }
}
