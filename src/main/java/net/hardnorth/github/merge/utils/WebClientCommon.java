package net.hardnorth.github.merge.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.exception.RestServiceException;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class WebClientCommon {
    private static final int FAILED_DEPENDENCY = HttpStatus.SC_FAILED_DEPENDENCY;

    public static <T> Response<T> executeServiceCall(Call<T> request) {
        try {
            Response<T> result = request.execute();
            if (!result.isSuccessful()) {
                if (result.code() >= HttpStatus.SC_BAD_REQUEST) {
                    JsonObject errorResponse = parseErrorBodyIfValid(result.headers(), result.errorBody());
                    String message = "Downstream service error: " + result.code() + " " + result.message();
                    if (errorResponse != null) {
                        throw new RestServiceException(message, result.code(), errorResponse);
                    } else {
                        throw new HttpException(message, FAILED_DEPENDENCY);
                    }
                }
                throw new HttpException("Unexpected upstream service response: " + result.code() + " " + result.message(), FAILED_DEPENDENCY);
            }
            return result;
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage(), e);
        }
    }

    private static JsonObject parseErrorBodyIfValid(Headers headers, ResponseBody body) {
        if (headers == null || body == null) {
            return null;
        }

        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON)) {
            return null;
        }

        String bodyStr = IoUtils.readInputStreamToString(body.byteStream());
        if (bodyStr == null) {
            return null;
        }

        try {
            JsonElement result = JsonParser.parseString(bodyStr);
            if (result.isJsonObject()) {
                return result.getAsJsonObject();
            }
            return null;
        } catch (JsonSyntaxException ignore) {
            return null;
        }
    }
}
