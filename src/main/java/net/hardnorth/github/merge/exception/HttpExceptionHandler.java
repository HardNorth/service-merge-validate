package net.hardnorth.github.merge.exception;

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
public class HttpExceptionHandler implements ExceptionMapper<HttpException> {
    private static final Logger LOGGER = Logger.getLogger(HttpExceptionHandler.class);

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(HttpException exception) {
        String error = "Downstream service call error";
        int status = exception.getCode();
        LOGGER.warn(simpleFormat("Downstream error: '{}'. ", error), exception);
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
