package net.hardnorth.github.merge.utils;

import com.google.gson.JsonElement;
import org.apache.http.HttpHeaders;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.hardnorth.github.merge.utils.WebServiceCommon.GSON;
import static net.hardnorth.github.merge.utils.WebServiceCommon.asString;

public class WebExceptionUtils {
    private WebExceptionUtils() {
    }

    public static JsonElement getExceptionOutput(String path, int status, String error, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", new Date());
        result.put("status_code", status);
        result.put("message", message);
        result.put("request_path", path);
        result.put("error", error);
        return GSON.toJsonTree(result);
    }

    public static JsonElement getExceptionOutput(UriInfo uriInfo, int status, String errorMessage, String message) {
        return getExceptionOutput(uriInfo.getPath(), status, errorMessage, message);
    }

    public static Response getExceptionResponse(UriInfo uriInfo, int status, String errorMessage, Exception exception) {
        return getExceptionResponse(uriInfo, status, Collections.emptyMap(), errorMessage, exception);
    }

    public static Response getExceptionResponse(UriInfo uriInfo, int status, Map<String, String> additionalHeaders,
                                                String errorMessage, Exception exception) {
        Response.ResponseBuilder responseBuilder = Response.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE)
                .entity(asString(getExceptionOutput(uriInfo, status, errorMessage, exception.getLocalizedMessage())));
        additionalHeaders.forEach(responseBuilder::header);
        return responseBuilder.build();
    }
}
