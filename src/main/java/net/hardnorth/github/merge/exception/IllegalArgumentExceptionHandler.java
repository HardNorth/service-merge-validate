package net.hardnorth.github.merge.exception;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static net.hardnorth.github.merge.utils.WebExceptionUtils.getExceptionResponse;

@Provider
public class IllegalArgumentExceptionHandler implements ExceptionMapper<IllegalArgumentException> {
    private static final Logger LOGGER = Logger.getLogger(IllegalArgumentExceptionHandler.class);
    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String error = "Invalid input parameter";
        int status = HttpStatus.SC_BAD_REQUEST;
        LOGGER.warn(exception.getMessage(), exception);
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
