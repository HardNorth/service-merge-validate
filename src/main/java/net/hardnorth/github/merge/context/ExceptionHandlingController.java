/*
 * Copyright (C) 2019 Epic Games, Inc. All Rights Reserved.
 */

package net.hardnorth.github.merge.context;

import com.google.gson.JsonElement;
import net.hardnorth.github.merge.web.AuthenticationException;
import net.hardnorth.github.merge.web.ConnectionException;
import net.hardnorth.github.merge.web.HttpException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Collections;
import java.util.logging.Logger;

import static net.hardnorth.github.merge.utils.StringUtils.simpleFormat;
import static net.hardnorth.github.merge.utils.WebServiceCommon.asString;
import static net.hardnorth.github.merge.utils.WebServiceCommon.getExceptionOutput;

@ControllerAdvice
public class ExceptionHandlingController extends ResponseEntityExceptionHandler
{
    private static final Logger LOGGER = Logger.getLogger("ExceptionHandler");

    private static final HttpHeaders EXCEPTION_HEADERS =
            new HttpHeaders(new LinkedMultiValueMap<>(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE))));

    @NonNull
    private ResponseEntity<Object> getErrorResponseEntity(@NonNull WebRequest request, @NonNull HttpStatus errorStatus,
                                                          @NonNull String error, @NonNull String message)
    {
        return getErrorResponseEntity(getExceptionOutput(request, errorStatus, error, message), errorStatus);
    }

    @NonNull
    private ResponseEntity<Object> getErrorResponseEntity(@NonNull JsonElement responseBody, @NonNull HttpStatus errorStatus)
    {
        return new ResponseEntity<>(asString(responseBody), EXCEPTION_HEADERS, errorStatus);
    }

    private ResponseEntity<Object> processInvalidInput(Exception ex, WebRequest request, HttpStatus status, String error)
    {
        LOGGER.info(simpleFormat("Bad request: '{}'. {}: '{}'\n{}", error, ex.getClass().getSimpleName(),
                ex.getLocalizedMessage(), ExceptionUtils.getStackTrace(ex)));
        return getErrorResponseEntity(request, status, error, ex.getLocalizedMessage());
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatus status, @NonNull WebRequest request)
    {
        String error = ex.getParameterName() + " parameter is missing";
        return processInvalidInput(ex, request, status, error);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            @NonNull MissingServletRequestPartException ex, @NonNull HttpHeaders headers, @NonNull HttpStatus status,
            @NonNull WebRequest request)
    {
        String error = ex.getRequestPartName() + " part is missing";
        return processInvalidInput(ex, request, status, error);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleServletRequestBindingException(
            @NonNull ServletRequestBindingException ex, @NonNull HttpHeaders headers, @NonNull HttpStatus status,
            @NonNull WebRequest request)
    {

        String error;
        if (ex instanceof MissingRequestHeaderException)
        {
            error = ((MissingRequestHeaderException) ex).getHeaderName() + " header is missing";
        }
        else
        {
            error = "Binding error";
        }

        return processInvalidInput(ex, request, status, error);
    }

    @Override
    @NonNull
    public ResponseEntity<Object> handleNoHandlerFoundException(
            @NonNull NoHandlerFoundException ex, @NonNull HttpHeaders headers, @NonNull HttpStatus status,
            @NonNull WebRequest request)
    {
        String error = "Not Found";
        return processInvalidInput(ex, request, status, error);
    }

    @ExceptionHandler(ConnectionException.class)
    public ResponseEntity<Object> rateLimiterFailed(ConnectionException ex, WebRequest request)
    {
        String error = "Downstream service connection error";
        LOGGER.warning(simpleFormat("Connection Error: '{}'. {}: '{}'\n{}", error, ex.getClass().getSimpleName(),
                ex.getLocalizedMessage(), ExceptionUtils.getStackTrace(ex)));
        return getErrorResponseEntity(request, HttpStatus.FAILED_DEPENDENCY, error, ex.getLocalizedMessage());
    }

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<Object> downstreamHttpException(HttpException ex, WebRequest request)
    {
        String error = "Downstream service error";
        HttpStatus status = HttpStatus.valueOf(ex.getCode());
        LOGGER.info(simpleFormat("Downstream error: '{}'. {}: '{}'", error, ex.getClass().getSimpleName(),
                ex.getLocalizedMessage()));
        return getErrorResponseEntity(request, status, error, ex.getLocalizedMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArgument(IllegalArgumentException ex, WebRequest request)
    {
        String error = "Invalid input parameter";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return processInvalidInput(ex, request, status, error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> illegalArgument(AuthenticationException ex, WebRequest request)
    {
        String error = "Authentication failed";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return processInvalidInput(ex, request, status, error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> anyRuntime(RuntimeException ex, WebRequest request)
    {
        String error = "Unknown exception occurred";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        LOGGER.severe(simpleFormat("Error: '{}'. {}: '{}'\n{}", error, ex.getClass().getSimpleName(),
                ex.getLocalizedMessage(), ExceptionUtils.getStackTrace(ex)));
        return getErrorResponseEntity(request, status, error, ex.getLocalizedMessage());
    }
}
