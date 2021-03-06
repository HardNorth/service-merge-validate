package net.hardnorth.github.merge.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import static net.hardnorth.github.merge.utils.WebExceptionUtils.getExceptionResponse;
import static net.hardnorth.github.merge.utils.StringUtils.simpleFormat;

@Provider
public class ConnectionExceptionHandler implements ExceptionMapper<ConnectionException> {
    private static final Logger LOGGER = Logger.getLogger(ConnectionExceptionHandler.class);

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConnectionException exception) {
        String error = "Downstream service connection error";
        int status = HttpStatus.SC_FAILED_DEPENDENCY;
        LOGGER.warn(simpleFormat("Connection Error: '{}'. {}: '{}'\n{}",
                error,
                exception.getClass().getSimpleName(),
                exception.getLocalizedMessage(),
                ExceptionUtils.getStackTrace(exception)
        ));
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
