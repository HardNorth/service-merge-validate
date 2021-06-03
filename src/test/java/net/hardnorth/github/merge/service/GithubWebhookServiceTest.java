package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import net.hardnorth.github.merge.model.hook.PushRequest;
import net.hardnorth.github.merge.service.impl.GithubWebhookService;
import net.hardnorth.github.merge.utils.IoUtils;
import net.hardnorth.github.merge.utils.WebServiceCommon;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GithubWebhookServiceTest {

    public static final String APPLICATION_NAME = "merge-validate";

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
        when(github.authenticateInstallation(anyString(), anyLong())).thenReturn(Pair.of(UUID.randomUUID().toString(), cal.getTime()));
    }

    @Test
    public void test_merge_path() {
        String request = IoUtils.readInputStreamToString(getClass().getClassLoader()
                .getResourceAsStream("hook/new_branch.json"), StandardCharsets.UTF_8);
        webhook.processPush(WebServiceCommon.deserializeJson(request, PushRequest.class));

        verify(mergeValidate).merge(anyString(), eq("HardNorth"), eq("agent-java-testNG"),
                eq("merge-validate-develop"), eq("develop"));
    }
}
