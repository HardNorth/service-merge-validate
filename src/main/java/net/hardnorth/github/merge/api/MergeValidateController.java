package net.hardnorth.github.merge.api;

import io.quarkus.security.AuthenticationFailedException;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.model.github.hook.EventCheckRun;
import net.hardnorth.github.merge.model.github.hook.EventInstallation;
import net.hardnorth.github.merge.model.github.hook.EventPullRequest;
import net.hardnorth.github.merge.model.github.hook.EventPush;
import net.hardnorth.github.merge.service.GithubWebhook;
import net.hardnorth.github.merge.service.SecretManager;
import net.hardnorth.github.merge.utils.WebServiceCommon;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;

@Path("/")
public class MergeValidateController {
    private static final Logger LOGGER = Logger.getLogger(MergeValidateController.class);

    private final byte[] webhookSecret;
    private final GithubWebhook webhook;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateController(@ConfigProperty(name = PropertyNames.GITHUB_WEBHOOK_TOKEN_SECRET) String webhookSecretKey,
                                   SecretManager secretManager, GithubWebhook webhookService) {
        webhookSecret = secretManager.getRawSecret(webhookSecretKey);
        webhook = webhookService;
    }

    @GET
    @Path("healthcheck")
    @Produces(MediaType.TEXT_PLAIN)
    public Response healthCheck() {
        return WebServiceCommon.healthCheck(this);
    }

    @POST
    @Path("webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces
    public void webhookAction(@HeaderParam(value = "x-github-event") final String event,
                              @HeaderParam(value = "x-hub-signature-256") final String signature, final String body) {
        byte[] rawSignature;
        try {
            rawSignature = Hex.decodeHex(signature.substring("sha256=".length()));
        } catch (DecoderException e) {
            throw new IllegalArgumentException("Invalid signature format: " + e.getMessage(), e);
        }
        if (!WebServiceCommon.validateSha256Signature(rawSignature, webhookSecret, body.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthenticationFailedException("Invalid signature");
        }
        LOGGER.infof("Got a '%s' webhook request:\n%s", event, body);

        switch (event) {
            case "installation":
                webhook.processInstallation(WebServiceCommon.deserializeJson(body, EventInstallation.class));
                break;
            case "push":
                webhook.processPush(WebServiceCommon.deserializeJson(body, EventPush.class));
                break;
            case "pull_request":
                webhook.processPull(WebServiceCommon.deserializeJson(body, EventPullRequest.class));
                break;
            case "check_run":
                webhook.processCheckRun(WebServiceCommon.deserializeJson(body, EventCheckRun.class));
                break;
            case "check_suite":
                break;
            default:
                throw new IllegalArgumentException("Unknown webhook event: " + event);
        }
    }
}
