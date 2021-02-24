package net.hardnorth.github.merge.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.impl.MergeValidateService;
import net.hardnorth.github.merge.utils.IoUtils;
import okhttp3.Headers;
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

public class MergeValidateServiceTest {

    private static final String MERGE_FILE_NAME = ".merge-validate";

    private static final Gson GSON = new Gson();

    private final GithubApiClient github = mock(GithubApiClient.class);


    private final MergeValidate service =
            new MergeValidateService(github, MERGE_FILE_NAME, new Charset(StandardCharsets.UTF_8), 512000);


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
        when(github.getContent(anyString(), anyString(), eq(path), eq("dest"))).thenReturn(call);
        when(response.body()).thenReturn(responseBody);
        when(response.headers()).thenReturn(Headers.of());
        return Pair.of(call, response);
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    public <T extends HttpException> void verify_github_bad_merge_file_responses(String file, Class<T> exception, int status, String message) throws IOException {
        mockContentCall("", GSON.fromJson(IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream(file)), JsonElement.class));
        T result = Assertions.assertThrows(exception, () -> service.merge("auth", "HardNorth/test", "source", "dest"));
        assertThat(result.getCode(), Matchers.equalTo(status));
        assertThat(result.getMessage(), Matchers.endsWith(message));
    }

    @Test
    public void verify_github_merge_success() throws IOException {
        mockContentCall("", GSON.fromJson(
                IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("github/file_list_merge_file.json")),
                JsonElement.class
        ));

        mockContentCall(MERGE_FILE_NAME, GSON.fromJson(
                IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("github/merge_file_default.json")),
                JsonElement.class
        ));

        service.merge("auth", "HardNorth/test", "source", "dest");
    }

}
