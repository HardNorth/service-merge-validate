package net.hardnorth.github.merge.api;

import io.quarkus.security.AuthenticationFailedException;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.model.Charset;
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

    private final java.nio.charset.Charset charset;
    private final byte[] webhookSecret;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateController(SecretManager secretManager, Charset serviceCharset,
                                   @ConfigProperty(name = PropertyNames.GITHUB_WEBHOOK_TOKEN_SECRET) String webhookSecretKey) {
        charset = serviceCharset.getValue();
        webhookSecret = secretManager.getRawSecret(webhookSecretKey);
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
    public void webhookAction(@HeaderParam(value = "x-hub-signature-256") String signature, String body) {
        byte[] rawSignature;
        try {
            rawSignature = Hex.decodeHex(signature.substring("sha256=".length()));
        } catch (DecoderException e) {
            throw new IllegalArgumentException("Invalid signature format: " + e.getMessage(), e);
        }
        if(!WebServiceCommon.validateSha256Signature(rawSignature, webhookSecret, body.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthenticationFailedException("Invalid signature");
        }
        LOGGER.info("Got a webhook request:\n" + body); // TODO: finish
    }
}
