package net.hardnorth.github.merge.exception;

import java.util.Arrays;

public class HttpException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;
    private byte[] bodyBytes;

    public HttpException(String message, int responseCode) {
        super(message);
        code = responseCode;
    }

    public HttpException(String message, int responseCode, Throwable cause) {
        super(message, cause);
        code = responseCode;
    }

    public HttpException(String message, int responseCode, byte[] responseBytes, Throwable cause) {
        this(message, responseCode, cause);
        bodyBytes = Arrays.copyOf(responseBytes, responseBytes.length);
    }

    public int getCode() {
        return code;
    }

    public byte[] getBodyBytes() {
        return Arrays.copyOf(bodyBytes, bodyBytes.length);
    }

    public String toString() {
        return "HTTP Error: " + getCode() + " - " + getMessage();
    }
}
