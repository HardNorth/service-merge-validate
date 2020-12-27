package net.hardnorth.github.merge.api;

import net.hardnorth.github.merge.service.AuthorizationService;
import net.hardnorth.github.merge.service.MergeValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController("merge-validate")
public class MergeValidateController {
	private static final List<String> scopes = Arrays.asList("repo", "user:email");
	private static final String clientId = "Iv1.556c0a74a6200c54";
	private static final String redirectUriPattern = "https://merge.hardnorth.net/integration/result/%s";

	private final AuthorizationService auth;
	private final MergeValidateService merge;

	@Autowired
	public MergeValidateController(AuthorizationService authorizationService, MergeValidateService mergeValidateService) {
		auth = authorizationService;
		merge = mergeValidateService;
	}

	@GetMapping("healthcheck")
	public ResponseEntity<String> healthCheck() {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);

		return ResponseEntity.ok().headers(responseHeaders).body(this.getClass().getAnnotation(RestController.class).value() + ": OK");
	}

	@PostMapping
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

	@PutMapping("merge/{from}/{to}")
	public void merge(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable("from") String from,
			@PathVariable("to") String to) {

	}

}
