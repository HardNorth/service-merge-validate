package net.hardnorth.github.merge.exception;

import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

public class RestServiceException
        extends HttpException {
    private JsonObject body;

    public RestServiceException(String message, int code, JsonObject bodyElement) {
        super(message, code);
        this.body = bodyElement;
    }

    public JsonObject getBody() {
        return body;
    }

    @Override
    public byte[] getBodyBytes() {
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return String.format("%s Response Body:%n%s", super.toString(), body);
    }
}