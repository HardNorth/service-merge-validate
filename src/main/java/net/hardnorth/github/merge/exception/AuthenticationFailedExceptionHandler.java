package net.hardnorth.github.merge.exception;

import io.quarkus.security.AuthenticationFailedException;
import org.apache.http.HttpStatus;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static net.hardnorth.github.merge.utils.WebExceptionUtils.getExceptionResponse;

@Provider
public class AuthenticationFailedExceptionHandler implements ExceptionMapper<AuthenticationFailedException> {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        String error = "Authentication failed";
        int status = HttpStatus.SC_BAD_REQUEST;
        return getExceptionResponse(uriInfo, status, error, exception);
    }
}
