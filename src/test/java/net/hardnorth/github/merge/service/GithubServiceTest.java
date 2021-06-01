package net.hardnorth.github.merge.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.CommitDifference;
import net.hardnorth.github.merge.model.FileChange;
import net.hardnorth.github.merge.service.impl.GithubService;
import net.hardnorth.github.merge.utils.IoUtils;
import okhttp3.Headers;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GithubServiceTest {
    public static final String MERGE_FILE_NAME = ".merge-validate";

    private static final Gson GSON = new Gson();

    private final GithubApiClient githubApiClient = mock(GithubApiClient.class);

    public final Github github =
            new GithubService(githubApiClient, 512000, new Charset(StandardCharsets.UTF_8));

    @SuppressWarnings({"unchecked"})
    private void mockContentCall(String path, JsonElement responseBody) throws IOException {
        Call<JsonElement> call = mock(Call.class);
        Response<JsonElement> response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(Boolean.TRUE);
        when(githubApiClient.getContent(anyString(), anyString(), anyString(), eq(path), eq("dest"))).thenReturn(call);
        when(response.body()).thenReturn(responseBody);
        when(response.headers()).thenReturn(Headers.of());
    }

    private String readFileString(String file) {
        return IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream(file), StandardCharsets.UTF_8);
    }

    public static Iterable<Object[]> invalidConfigurationFileResponses() {
        return Arrays.asList(new Object[]{"github/file_list_no_merge_file.json", HttpException.class, HttpStatus.SC_BAD_REQUEST, "no configuration file found"},
                new Object[]{"github/file_list_no_merge_file_large.json", HttpException.class, HttpStatus.SC_BAD_REQUEST, "no configuration file found"},
                new Object[]{"github/file_list_merge_file_too_large.json", HttpException.class, HttpStatus.SC_REQUEST_TOO_LONG, "file size limit exceed"});
    }

    @ParameterizedTest
    @MethodSource("invalidConfigurationFileResponses")
    public <T extends HttpException> void verify_github_bad_merge_file_responses(String file, Class<T> exception, int status, String message) throws IOException {
        mockContentCall("", GSON.fromJson(readFileString(file), JsonElement.class));
        T result = Assertions.assertThrows(exception, () -> github.getFileContent("auth", "HardNorth", "test", "dest", MERGE_FILE_NAME));
        assertThat(result.getCode(), equalTo(status));
        assertThat(result.getMessage(), Matchers.endsWith(message));
    }

    @Test
    public void verify_github_bad_merge_file_responses() throws IOException {
        mockContentCall("", GSON.fromJson(readFileString("github/file_list_merge_file.json"), JsonElement.class));
        String content = readFileString("github/merge_file_default.json");
        mockContentCall(MERGE_FILE_NAME, GSON.fromJson(content, JsonElement.class));

        String result = new String(github.getFileContent("auth", "HardNorth", "test", "dest", MERGE_FILE_NAME), StandardCharsets.UTF_8);
        String expected = readFileString("file/default.txt").replace("\r", "");
        assertThat(result, equalTo(expected));
    }

    @SuppressWarnings("unchecked")
    private void mockBranchResponse(String branch, JsonObject responseBody) throws IOException {
        Call<JsonObject> branchCall = mock(Call.class);
        Response<JsonObject> branchResponse = mock(Response.class);
        when(branchCall.execute()).thenReturn(branchResponse);
        when(branchResponse.isSuccessful()).thenReturn(Boolean.TRUE);
        when(githubApiClient.getBranch(anyString(), anyString(), anyString(), eq(branch))).thenReturn(branchCall);
        when(branchResponse.body()).thenReturn(responseBody);
        when(branchResponse.headers()).thenReturn(Headers.of());
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private void mockChangesCall(String source, String dest, JsonObject responseBody) throws IOException {
        String masterBranchResponseStr = readFileString("github/get_master_branch.json");
        JsonObject masterBranchResponse = GSON.fromJson(masterBranchResponseStr, JsonObject.class);
        mockBranchResponse(dest, masterBranchResponse);

        String developBranchResponseStr = readFileString("github/get_develop_branch.json");
        JsonObject developBranchResponse = GSON.fromJson(developBranchResponseStr, JsonObject.class);
        mockBranchResponse(source, developBranchResponse);

        Call<JsonObject> call = mock(Call.class);
        Response<JsonObject> response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(Boolean.TRUE);
        when(githubApiClient.compareCommits(anyString(), anyString(), anyString(), eq(masterBranchResponse.getAsJsonObject("commit").getAsJsonPrimitive("sha").getAsString()),
                eq(developBranchResponse.getAsJsonObject("commit").getAsJsonPrimitive("sha").getAsString()))).thenReturn(call);

        when(response.body()).thenReturn(responseBody);
        when(response.headers()).thenReturn(Headers.of());
    }

    public static Iterable<Object[]> diffResponses() {
        return Arrays.asList(new Object[]{"github/change_list_source_behind.json", new CommitDifference(0, 5,
                        Collections.emptyList())},
                new Object[]{"github/change_list_illegal_changes.json", new CommitDifference(5, 0,
                        Arrays.asList(new FileChange(FileChange.Type.CHANGED, ".github/workflows/release.yml"),
                                new FileChange(FileChange.Type.ADDED, ".merge-validate"),
                                new FileChange(FileChange.Type.CHANGED, "README.md")))});
    }

    @ParameterizedTest
    @MethodSource("diffResponses")
    public void verify_github_changes_responses(String file, CommitDifference expected) throws IOException {
        String changesStr = readFileString(file);
        mockChangesCall("develop", "master", GSON.fromJson(changesStr, JsonObject.class));

        CommitDifference result = github.listChanges("auth", "HardNorth", "test", "develop", "master");

        assertThat(result.getAheadBy(), equalTo(expected.getAheadBy()));
        assertThat(result.getBehindBy(), equalTo(expected.getBehindBy()));
        assertThat(result.getCommits(), hasSize(expected.getCommits().size()));
        IntStream.range(0, result.getCommits().size()).forEach(i -> {
            FileChange act = result.getCommits().get(i);
            FileChange exp = expected.getCommits().get(i);
            assertThat(act.getType(), sameInstance(exp.getType()));
            assertThat(act.getName(), equalTo(exp.getName()));
        });
    }
}
