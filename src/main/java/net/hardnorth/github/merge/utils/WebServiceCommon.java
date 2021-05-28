package net.hardnorth.github.merge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WebServiceCommon {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    public static final String HMAC_ALGORITHM = "HmacSHA256";

    public static final Gson GSON = new Gson().newBuilder().serializeNulls().setDateFormat(DATE_FORMAT).create();

    /**
     * Reads simple class name from {@code controller} and generate standard Health Check response based on the component name and constant
     * 'OK' literal.
     *
     * @param controller a controller object to get component name
     * @return web-service standard response
     */
    public static Response healthCheck(Object controller) {
        return Response.status(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .encoding(StandardCharsets.UTF_8.name())
                .entity(controller.getClass().getSimpleName() + ": OK").build();
    }

    public static Response performRedirect(String where) {
        return Response.status(HttpStatus.SC_MOVED_TEMPORARILY).header(HttpHeaders.LOCATION, where).build();
    }

    public static JsonArray asJson(Object[] array) {
        return GSON.toJsonTree(array).getAsJsonArray();
    }

    public static JsonObject asJson(Object object) {
        return GSON.toJsonTree(object).getAsJsonObject();
    }

    public static String asString(JsonElement json) {
        return GSON.toJson(json);
    }

    @Nullable
    public static String getAuthToken(@Nonnull String authHeader) {
        int separator = authHeader.indexOf(' ');
        if (separator <= 0) {
            return null;
        }
        String type = authHeader.substring(0, separator);
        String token = authHeader.substring(separator + 1).trim();
        if (!"bearer".equalsIgnoreCase(type) || token.isEmpty()) {
            return null;
        }
        return token;
    }

    public static boolean validateSha256Signature(byte[] signature, byte[] secret, byte[] body) {
        Mac mac;
        try {
            mac = Mac.getInstance(HMAC_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to find " + HMAC_ALGORITHM + " algorithm:", e);
        }
        SecretKeySpec key = new SecretKeySpec(secret, HMAC_ALGORITHM);
        try {
            mac.init(key);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key:", e);
        }
        byte[] actualSignature = mac.doFinal(body);
        return MessageDigest.isEqual(signature, actualSignature);
    }
}
