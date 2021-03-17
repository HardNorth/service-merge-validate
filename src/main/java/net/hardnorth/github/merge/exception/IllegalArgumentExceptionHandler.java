package net.hardnorth.github.merge.exception;

import org.apache.http.HttpStatus;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.function.Predicate;
import org.jboss.logging.Logger;

import static net.hardnorth.github.merge.utils.WebExceptionUtils.getExceptionResponse;
import static org.apache.commons.lang3.exception.ExceptionUtils.*;

@Provider
public class IllegalArgumentExceptionHandler implements ExceptionMapper<IllegalArgumentException> {
    private static final Logger LOGGER = Logger.getLogger(IllegalArgumentExceptionHandler.class);

    private static final Predicate<StackTraceElement> IS_BASE64_ELEMENT = e -> e.getClassName().endsWith("Base64");

    private static final Predicate<Exception> IS_BASE64_EXCEPTION = e -> Arrays.stream(e.getStackTrace()).anyMatch(IS_BASE64_ELEMENT);

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String error = "Invalid input parameter";
        int status = HttpStatus.SC_BAD_REQUEST;
        if (IS_BASE64_EXCEPTION.test(exception)) {
            LOGGER.warn("Unable to decode Base64: " + exception.getMessage(), exception);
        }
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
