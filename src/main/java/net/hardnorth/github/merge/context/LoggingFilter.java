package net.hardnorth.github.merge.context;

import io.vertx.core.http.HttpServerRequest;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.utils.IdUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Provider
public class LoggingFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @ConfigProperty(name = PropertyNames.GITHUB_LOG)
    Boolean logRequests;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Charset charset;

    @Override
    public void filter(ContainerRequestContext context) {
        if (!logRequests) {
            return;
        }

        final String requestId = IdUtils.generateId();
        final String method = context.getMethod();
        final String path = info.getPath();
        final String address = request.remoteAddress().toString();

        LOGGER.infof("[%s] Request %s %s from IP %s", requestId, method, path, address);
        String headers = StreamSupport
                .stream(request.headers().spliterator(), false)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        LOGGER.infof("[%s] Headers: \n %s", requestId, headers);
        request.bodyHandler(b -> {
            if (b.length() > 0) {
                LOGGER.infof("[%s] Body: \n %s", requestId, b.toString(charset.getValue()));
            }
        });
    }
}
