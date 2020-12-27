package net.hardnorth.github.merge.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

@RestController("merge-validate")
public class MergeValidateController {

	@GetMapping("healthcheck")
	public ResponseEntity<String> healthCheck()
	{
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);

		return ResponseEntity.ok()
				.headers(responseHeaders)
				.body(this.getClass().getAnnotation(RestController.class).value() + ": OK");
	}

	@PostMapping()
	public void ping(@RequestBody String body) {
		System.out.println(body);
	}

	@PostMapping("authorize")
	public void authorize(@RequestBody String body) {

	}

	@PostMapping("integration")
	public ResponseEntity<Void> createIntegration(@RequestBody String repositoryUrl) {
		return null; // redirect URL
	}

	@GetMapping("return")
	public ResponseEntity<Void> integrationResult() {
		return null; // return UUID
	}

	@PutMapping("merge/{uuid}/{from}/{to}")
	public void merge(@PathVariable String uuid, @PathVariable String from, @PathVariable String to) {

	}

}
