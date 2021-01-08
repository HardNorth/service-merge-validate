package net.hardnorth.github.merge.service;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GithubOAuthServiceTest {
	public static final String PROJECT_ID = "test";
	public static final int SERVICE_PORT = 8889;
	public static final String SERVICE_URL = "http://localhost:" + SERVICE_PORT;
	public static final String APPLICATION_NAME = "test-application";
	public static final String CLIENT_ID = "test-client-id";

	private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

	@BeforeAll
	public static void datastoreHelperInit() throws IOException, InterruptedException {
		HELPER.start();
	}

	@AfterAll
	public static void datastoreHelperTearDown() throws InterruptedException, TimeoutException, IOException {
		HELPER.stop();
	}

	private Datastore datastore;
	private GithubOAuthService service;

	@BeforeEach
	public void setUp() {
		datastore = HELPER.getOptions().toBuilder().setProjectId(PROJECT_ID).build().getService();
		service = new GithubOAuthService(datastore, SERVICE_URL, APPLICATION_NAME, CLIENT_ID);
	}

	@Test
	public void verify_createIntegration_url() throws MalformedURLException {
		String urlStr = service.createIntegration();

		assertThat(urlStr, startsWith("https://github.com/login/oauth/authorize"));

		URL url = new URL(urlStr);
		Map<String, String> query = Arrays.stream(url.getQuery().split("&"))
				.collect(Collectors.toMap(s -> s.split("=")[0], s -> s.split("=")[1]));
		assertThat(query, hasEntry(equalTo("redirect_uri"),
				startsWith(URLEncoder.encode(SERVICE_URL + "/integration/result/", StandardCharsets.UTF_8))
		));
		assertThat(query, hasEntry(equalTo("state"), not(emptyOrNullString())));
		assertThat(query, hasEntry(equalTo("client_id"), equalTo(URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8))));
		assertThat(query, hasEntry(equalTo("scope"), equalTo(URLEncoder.encode("repo user:email", StandardCharsets.UTF_8))));
	}

	@Test
	public void verify_createIntegration_database_entry_creation() throws MalformedURLException {
		String urlStr = service.createIntegration();

		URL url = new URL(urlStr);
		Map<String, String> urlQuery = Arrays.stream(url.getQuery().split("&"))
				.collect(Collectors.toMap(s -> s.split("=")[0], s -> URLDecoder.decode(s.split("=")[1], StandardCharsets.UTF_8)));
		String authUuid = urlQuery.get("redirect_uri").substring(urlQuery.get("redirect_uri").lastIndexOf('/') + 1);
		EntityQuery query = Query.newEntityQueryBuilder()
				.setKind(APPLICATION_NAME + "-" + "github-oauth")
				.setFilter(StructuredQuery.PropertyFilter.eq("authUuid", authUuid))
				.build();

		QueryResults<Entity> result = datastore.run(query);
		List<Entity> entities = StreamSupport.stream(Spliterators.spliteratorUnknownSize(result, Spliterator.ORDERED), false)
				.collect(Collectors.toList());
		assertThat(entities, hasSize(1));
		Entity entity = entities.get(0);
		assertThat(entity.getTimestamp("expires").toDate(), greaterThan(Calendar.getInstance().getTime()));
	}
}
