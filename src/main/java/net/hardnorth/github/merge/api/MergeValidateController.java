package net.hardnorth.github.merge.api;

import net.hardnorth.github.merge.service.AuthorizationService;
import net.hardnorth.github.merge.service.MergeValidateService;
import net.hardnorth.github.merge.utils.WebServiceCommon;
import net.hardnorth.github.merge.web.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

import static java.util.Optional.ofNullable;

@RestController("merge-validate")
public class MergeValidateController {
	private static final List<String> scopes = Arrays.asList("repo", "user:email");
	private static final String clientId = "Iv1.556c0a74a6200c54";
	private static final String redirectUriPattern = "https://merge.hardnorth.net/integration/result/%s";

	private final AuthorizationService authService;
	private final MergeValidateService mergeService;

	@Autowired
	public MergeValidateController(AuthorizationService authorizationService, MergeValidateService mergeValidateService) {
		authService = authorizationService;
		mergeService = mergeValidateService;
	}

	@GetMapping("healthcheck")
	public ResponseEntity<String> healthCheck() {
		return WebServiceCommon.healthCheck(this);
	}

	@PostMapping("ping")
	public void ping(@RequestBody String body) {
		System.out.println(body);
	}

	@PostMapping("integration")
	public ResponseEntity<Void> createIntegration(@RequestParam String repositoryUrl) {
		// see: https://docs.github.com/en/free-pro-team@latest/developers/apps/authorizing-oauth-apps
		return null; // return redirect URL in headers, authUuid
	}

	@GetMapping("integration/result/{authUuid}")
	public ResponseEntity<String> integrationResult(@PathVariable("authUuid") String authUuid, @RequestParam("state") String state,
			@RequestParam("code") String code) {
		return null; // Repo UUID
	}

	@PutMapping("merge")
	public void merge(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam("repoUrl") String repoUrl,
			@RequestParam("from") String from, @RequestParam("to") String to) {
		String authToken = ofNullable(WebServiceCommon.getAuthToken(auth)).orElseThrow(() -> new IllegalArgumentException(
				"Unable to extract Authentication Token from header"));
		String githubToken = ofNullable(authService.getToken(authToken)).orElseThrow(AuthenticationException::new);

	}

}
