package net.hardnorth.github.merge.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.impl.MergeValidateService;
import net.hardnorth.github.merge.utils.IoUtils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MergeValidateServiceTest {

    private static final Gson GSON = new Gson();

    private final GithubApiClient github = mock(GithubApiClient.class);
    private final OkHttpClient http = mock(OkHttpClient.class);


    private final MergeValidate service =
            new MergeValidateService(github, http, ".merge-validate", new Charset(StandardCharsets.UTF_8), 2, 512000);

    @Test
    @SuppressWarnings("unchecked")
    public void verify_github_no_merge_file_response() throws IOException {
        Call<JsonElement> call = mock(Call.class);
        Response<JsonElement> response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(Boolean.TRUE);
        when(github.getContent(anyString(), anyString(), anyString(), eq("dest"))).thenReturn(call);
        when(response.body())
                .thenReturn(GSON.fromJson(IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("github/file_list_no_merge_file.txt")), JsonElement.class));
        when(response.headers()).thenReturn(Headers.of());


        HttpException result = Assertions.assertThrows(HttpException.class, () -> service.merge("auth", "HardNorth/test", "source", "dest"));
        assertThat(result.getMessage(), Matchers.endsWith("no configuration file found"));
    }


}
