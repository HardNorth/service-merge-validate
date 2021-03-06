package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.exception.RestServiceException;
import net.hardnorth.github.merge.model.github.hook.EventPullRequest;
import net.hardnorth.github.merge.model.github.hook.EventPush;
import net.hardnorth.github.merge.model.github.repo.BranchProtection;
import net.hardnorth.github.merge.model.github.repo.PullRequest;
import net.hardnorth.github.merge.service.impl.GithubWebhookService;
import net.hardnorth.github.merge.utils.IoUtils;
import net.hardnorth.github.merge.utils.WebClientCommon;
import net.hardnorth.github.merge.utils.WebServiceCommon;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GithubWebhookServiceTest {

    public static final String APPLICATION_NAME = "merge-validate";
    private static final String TOKENS_KIND = "tokens";
    private static final String TOKEN = "token";
    private static final String INSTALLATION_ID = "installation_id";

    public final Github github = mock(Github.class);
    public final MergeValidate mergeValidate = mock(MergeValidate.class);
    public final JWT jwt = mock(JWT.class);
    private final Datastore datastore = DatastoreOptions.newBuilder().setNamespace(APPLICATION_NAME).build().getService();
    public final GithubWebhook webhook = new GithubWebhookService(APPLICATION_NAME, github, mergeValidate, jwt, datastore);

    @BeforeEach
    public void setup() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        when(jwt.get()).thenReturn(UUID.randomUUID().toString());
        when(github.authenticateInstallation(anyString(), anyLong()))
                .thenReturn(Pair.of(UUID.randomUUID().toString(), cal.getTime()));
        when(github
                .createPullRequest(anyString(), anyString(),anyString(),anyString(),anyString(), anyString(), nullable(String.class)))
                .thenAnswer(a -> new Random().nextInt(1000000));
        when(github.getPullRequest(anyString(), anyString(), anyString(), anyInt())).thenReturn(new PullRequest());
    }

    @Test
    public void test_merge_path() {
        String request = IoUtils.readInputStreamToString(getClass().getClassLoader()
                .getResourceAsStream("hook/pr_labeled.json"), StandardCharsets.UTF_8);
        webhook.processPull(WebServiceCommon.deserializeJson(request, EventPullRequest.class));

        verify(mergeValidate).validate(anyString(), eq("HardNorth"), eq("agent-java-testNG"),
                eq("merge-validate-develop"), eq("develop"));
        verify(github).createReview(anyString(), eq("HardNorth"),
                eq("agent-java-testNG"), anyInt(), eq("APPROVE"), nullable(String.class));
    }

    @Test
    public void test_datastore_cache() {
        String requestStr = IoUtils.readInputStreamToString(getClass().getClassLoader()
                .getResourceAsStream("hook/pr_labeled.json"), StandardCharsets.UTF_8);
        EventPullRequest request = WebServiceCommon.deserializeJson(requestStr, EventPullRequest.class);
        long installationId = new Random().nextLong();
        request.getInstallation().setId(installationId);

        // Perform first call and ensure token was saved to cache
        webhook.processPull(request);

        EntityQuery query = Query
                .newEntityQueryBuilder()
                .setKind(TOKENS_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq(INSTALLATION_ID, installationId))
                .build();
        QueryResults<Entity> tokenResult = datastore.run(query);

        List<Entity> allTokens = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(tokenResult, Spliterator.SIZED),
                false
        ).collect(Collectors.toList());
        assertThat(allTokens, hasSize(1));

        String token = allTokens.get(0).getString(TOKEN);

        // Perform second call and ensure token was taken for cache
        webhook.processPull(request);

        tokenResult = datastore.run(query);
        allTokens = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(tokenResult, Spliterator.SIZED),
                false
        ).collect(Collectors.toList());
        assertThat(allTokens, hasSize(1));
        assertThat(allTokens.get(0).getString(TOKEN), equalTo(token));

        // Verify only one authentication called
        verify(github).authenticateInstallation(anyString(), eq(installationId));

        // Verify two merges performed
        verify(mergeValidate, times(2)).validate(endsWith(token), eq("HardNorth"),
                eq("agent-java-testNG"), eq("merge-validate-develop"), eq("develop"));
        verify(github, times(2)).createReview(endsWith(token), eq("HardNorth"),
                eq("agent-java-testNG"), anyInt(), eq("APPROVE"), nullable(String.class));
    }
}
