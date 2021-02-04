package net.hardnorth.github.merge.exception;

import io.quarkus.security.UnauthorizedException;
import org.apache.http.HttpStatus;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static net.hardnorth.github.merge.utils.ExceptionUtils.getExceptionResponse;

@Provider
public class UnauthorizedExceptionHandler implements ExceptionMapper<UnauthorizedException> {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(UnauthorizedException exception) {
        String error = "Unauthorized";
        int status = HttpStatus.SC_UNAUTHORIZED;
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
