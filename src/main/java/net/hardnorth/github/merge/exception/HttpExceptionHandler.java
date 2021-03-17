package net.hardnorth.github.merge.exception;

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
public class HttpExceptionHandler implements ExceptionMapper<HttpException> {
    private static final Logger LOGGER = Logger.getLogger(HttpExceptionHandler.class.getSimpleName());

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(HttpException exception) {
        String error = "Downstream service error";
        int status = exception.getCode();
        LOGGER.info(simpleFormat("Downstream error: '{}'. {}: '{}'", error, exception.getClass().getSimpleName(),
                exception.getLocalizedMessage()));
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
