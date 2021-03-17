package net.hardnorth.github.merge.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

import static net.hardnorth.github.merge.utils.WebExceptionUtils.getExceptionResponse;
import static net.hardnorth.github.merge.utils.StringUtils.simpleFormat;

@Provider
public class RuntimeExceptionHandler implements ExceptionMapper<RuntimeException> {
    private static final Logger LOGGER = Logger.getLogger(RuntimeExceptionHandler.class.getSimpleName());

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(RuntimeException exception) {
        String error = "Unknown exception occurred";
        int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        LOGGER.severe(simpleFormat("Error: '{}'. {}: '{}'\n{}",
                error,
                exception.getClass().getSimpleName(),
                exception.getLocalizedMessage(),
                ExceptionUtils.getStackTrace(exception)
        ));
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
