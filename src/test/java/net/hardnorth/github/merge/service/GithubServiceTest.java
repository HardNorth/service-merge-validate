package net.hardnorth.github.merge.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.impl.GithubOAuthService;
import net.hardnorth.github.merge.service.impl.GithubService;
import net.hardnorth.github.merge.utils.IoUtils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GithubServiceTest {
    public static final String MERGE_FILE_NAME = ".merge-validate";
    public static final String CLIENT_ID = "test-client-id";
    public static final String CLIENT_SECRET = "test-client-secret";

    private static final Gson GSON = new Gson();

    private final OkHttpClient httpClient = mock(OkHttpClient.class);
    private final GithubAuthClient githubAuthClient = mock(GithubAuthClient.class);
    private final GithubApiClient githubApiClient = mock(GithubApiClient.class);

    public final Github github = new GithubService(httpClient, githubAuthClient, githubApiClient, new GithubCredentials(CLIENT_ID, CLIENT_SECRET), 512000);

    public static Iterable<Object[]> invalidResponses() {
        return Arrays.asList(new Object[]{"github/file_list_no_merge_file.json", HttpException.class, HttpStatus.SC_BAD_REQUEST, "no configuration file found"},
                new Object[]{"github/file_list_no_merge_file_large.json", HttpException.class, HttpStatus.SC_BAD_REQUEST, "no configuration file found"},
                new Object[]{"github/file_list_merge_file_too_large.json", HttpException.class, HttpStatus.SC_REQUEST_TOO_LONG, "file size limit exceed"});
    }


    @SuppressWarnings("unchecked")
    private Pair<Call<JsonElement>, Response<JsonElement>> mockContentCall(String path, JsonElement responseBody) throws IOException {
        Call<JsonElement> call = mock(Call.class);
        Response<JsonElement> response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(Boolean.TRUE);
        when(githubApiClient.getContent(anyString(), anyString(), eq(path), eq("dest"))).thenReturn(call);
        when(response.body()).thenReturn(responseBody);
        when(response.headers()).thenReturn(Headers.of());
        return Pair.of(call, response);
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    public <T extends HttpException> void verify_github_bad_merge_file_responses(String file, Class<T> exception, int status, String message) throws IOException {
        mockContentCall("", GSON.fromJson(IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream(file)), JsonElement.class));
        T result = Assertions.assertThrows(exception, () -> github.getFileContent("auth", "HardNorth/test", "dest", MERGE_FILE_NAME));
        assertThat(result.getCode(), Matchers.equalTo(status));
        assertThat(result.getMessage(), Matchers.endsWith(message));
    }

    @Test
    public void verify_github_bad_merge_file_responses() throws IOException {
        mockContentCall("", GSON.fromJson(IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("github/file_list_merge_file.json")), JsonElement.class));
        String content = IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("github/merge_file_default.json"));
        mockContentCall(MERGE_FILE_NAME, GSON.fromJson(content, JsonElement.class));

        String result = new String(github.getFileContent("auth", "HardNorth/test", "dest", MERGE_FILE_NAME), StandardCharsets.UTF_8);
        String expected = IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("file/default.txt")).replace("\r", "");
        assertThat(result, Matchers.equalTo(expected));
    }
}
