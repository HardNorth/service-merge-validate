package net.hardnorth.github.merge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;

public class WebServiceCommon {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

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
}
