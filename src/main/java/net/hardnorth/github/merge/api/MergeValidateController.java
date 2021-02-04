package net.hardnorth.github.merge.api;

import io.quarkus.security.AuthenticationFailedException;
import net.hardnorth.github.merge.service.GithubOAuthService;
import net.hardnorth.github.merge.service.MergeValidateService;
import net.hardnorth.github.merge.utils.WebServiceCommon;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.util.Optional.ofNullable;

@Path("/")
public class MergeValidateController {
    private final GithubOAuthService authService;
    private final MergeValidateService mergeService;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateController(GithubOAuthService authorizationService, MergeValidateService mergeValidateService) {
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
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response createIntegration() {
        // see: https://docs.github.com/en/free-pro-team@latest/developers/apps/authorizing-oauth-apps
        return WebServiceCommon.performRedirect(authService.createIntegration()); // return redirect URL in headers, authUuid
    }

    @GET
    @Path("integration/result/{authUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response integrationResult(@PathParam("authUuid") String authUuid, @QueryParam("state") String state,
                                      @QueryParam("code") String code) {
        authService.authorize(authUuid, code, state);
        return null; // Repo UUID
    }

    @PUT
    @Path("merge")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public void merge(@HeaderParam(value = "Authorization") String auth, @QueryParam("repoUrl") String repoUrl,
                      @QueryParam("from") String from, @QueryParam("to") String to) {
        String authToken = ofNullable(WebServiceCommon.getAuthToken(auth)).orElseThrow(() -> new IllegalArgumentException(
                "Unable to extract Authentication Token from header"));
        String githubToken = ofNullable(authService.authenticate(authToken)).orElseThrow(AuthenticationFailedException::new);
        // TODO: implement
    }
}
