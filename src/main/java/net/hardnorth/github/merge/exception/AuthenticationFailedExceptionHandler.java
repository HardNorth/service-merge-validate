package net.hardnorth.github.merge.exception;

import io.quarkus.security.AuthenticationFailedException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Collections;

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
        int status = HttpStatus.SC_UNAUTHORIZED;

        return getExceptionResponse(uriInfo, status,
                Collections.singletonMap(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Access to API\""),
                error, exception);
    }
}
