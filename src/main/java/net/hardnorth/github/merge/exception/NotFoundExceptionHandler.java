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

import static net.hardnorth.github.merge.utils.ExceptionUtils.getExceptionResponse;
import static net.hardnorth.github.merge.utils.StringUtils.simpleFormat;

@Provider
public class NotFoundExceptionHandler implements ExceptionMapper<NotFoundException> {
    private static final Logger LOGGER = Logger.getLogger(NotFoundExceptionHandler.class.getSimpleName());

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(NotFoundException exception) {
        String error = "Not Found";
        int status = HttpStatus.SC_NOT_FOUND;
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
