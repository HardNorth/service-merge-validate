package net.hardnorth.github.merge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebServiceCommon
{
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    private static final Gson GSON = new Gson().newBuilder().serializeNulls().setDateFormat(DATE_FORMAT).create();

    /**
     * Reads {@link RestController} annotation from {@code controller} and generate standard Health Check response
     * based on the annotation value (component name) and constant 'OK' literal.
     *
     * @param controller a controller object to get component name
     * @return web-service standard response
     */
    public static ResponseEntity<String> healthCheck(Object controller)
    {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(controller.getClass().getAnnotation(RestController.class).value() + ": OK");
    }

    public static ResponseEntity<Void> performRedirect(String where)
    {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.LOCATION, where);

        return ResponseEntity.status(HttpStatus.FOUND).headers(responseHeaders).build();
    }


    public static JsonElement getExceptionOutput(String path, HttpStatus status, String error, String message)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", new Date());
        result.put("status_code", status.value());
        result.put("status_message", status.getReasonPhrase());
        result.put("message", message);
        result.put("request_path", path);
        result.put("error", error);
        return GSON.toJsonTree(result);
    }

    public static JsonElement getExceptionOutput(WebRequest request, HttpStatus status, String error, String message)
    {
        String path = null;
        if (request instanceof HttpServletRequest)
        {
            path = ((HttpServletRequest) request).getRequestURI();
        }
        else if (request instanceof ServletWebRequest)
        {
            path = ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return getExceptionOutput(path, status, error, message);
    }

    public static JsonArray asJson(Object[] array)
    {
        return GSON.toJsonTree(array).getAsJsonArray();
    }

    public static String asString(JsonElement json)
    {
        return GSON.toJson(json);
    }

    @Nullable
    public static String getAuthToken(@Nonnull String authHeader) {
        String decoded = new String(Base64.getDecoder().decode(authHeader), StandardCharsets.UTF_8);
        int separator = decoded.indexOf(' ');
        if(separator <= 0) {
            return null;
        }
        String type = decoded.substring(0, separator);
        String token = decoded.substring(separator + 1).trim();
        if (!"bearer".equalsIgnoreCase(type) || token.isEmpty()) {
            return null;
        }
        return token;
    }
}
