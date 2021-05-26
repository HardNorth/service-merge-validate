package net.hardnorth.github.merge.api;

import net.hardnorth.github.merge.service.MergeValidate;
import net.hardnorth.github.merge.service.OAuthService;
import net.hardnorth.github.merge.utils.WebServiceCommon;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;

import javax.annotation.Nonnull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;

@Path("/")
public class MergeValidateController {
    private static final Logger LOGGER = Logger.getLogger(MergeValidateController.class);

    private final OAuthService authService;
    private final MergeValidate mergeService;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateController(OAuthService authorizationService, MergeValidate mergeValidateService) {
        authService = authorizationService;
        mergeService = mergeValidateService;
    }

    @GET
    @Path("healthcheck")
    @Produces(MediaType.TEXT_PLAIN)
    public Response healthCheck() {
        return WebServiceCommon.healthCheck(this);
    }

    @POST
    @Path("integration")
    @Consumes
    @Produces
    public Response createIntegration() {
        // see: https://docs.github.com/en/free-pro-team@latest/developers/apps/authorizing-oauth-apps
        return WebServiceCommon.performRedirect(authService.createIntegration()); // return redirect URL in headers, authUuid
    }

    @GET
    @Path("integration/result")
    @Produces(MediaType.TEXT_PLAIN)
    public Response integrationResult(@Nonnull @QueryParam("authUuid") String authUuid, @Nonnull @QueryParam("state") String state,
                                      @Nonnull @QueryParam("code") String code) {
        String userToken = authService.authorize(authUuid, code, state);
        return Response.status(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .encoding(StandardCharsets.UTF_8.name())
                .entity(userToken).build();
    }

    @PUT
    @Path("merge")
    @Consumes
    @Produces
    public void merge(@HeaderParam(value = "Authorization") String auth, @QueryParam("user") String user,
                      @QueryParam("repo") String repo, @QueryParam("from") String from, @QueryParam("to") String to) {
        String authToken = WebServiceCommon.getAuthToken(auth);
        String githubToken = authService.authenticate(authToken);
        mergeService.merge(githubToken, user, repo, from, to);
    }

    @POST
    @Path("webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces
    public void webhookAction(@HeaderParam(value = "x-hub-signature-256") String signature, String body) {
        LOGGER.info("Got a webhook request:\n" + body); // TODO: finish
    }
}
