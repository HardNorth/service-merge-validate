package net.hardnorth.github.merge.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.GithubApiClient;
import net.hardnorth.github.merge.service.GithubAuthClient;
import net.hardnorth.github.merge.utils.WebClientCommon;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import retrofit2.Response;

import javax.annotation.Nonnull;
import java.util.Base64;

public class GithubService implements Github {

    private static final String DIRECTORY_DELIMITER = "/";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ACCESS_TOKEN_TYPE = "token_type";
    private static final String TYPE_FIELD = "type";
    private static final String NAME_FIELD = "name";
    private static final String CONTENT_FIELD = "content";
    private static final String SIZE_FIELD = "size";

    public static final RuntimeException INVALID_API_RESPONSE = new ConnectionException("Invalid response from Github API");
    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE
            = new HttpException("Unable to get merge configuration for target branch: invalid response", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_NO_FILE
            = new HttpException("Unable to get merge configuration for target branch: no configuration file found", HttpStatus.SC_BAD_REQUEST);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON
            = new HttpException("Unable to get merge configuration for target branch: response is not JSON", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_FILE_TOO_BIG
            = new HttpException("Unable to get merge configuration for target branch: file size limit exceed", HttpStatus.SC_REQUEST_TOO_LONG);

    private final OkHttpClient client;
    private final GithubAuthClient authClient;
    private final GithubApiClient apiClient;
    private final GithubCredentials credentials;
    private final long sizeLimit;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubService(OkHttpClient httpClient, GithubAuthClient githubAuthClient, GithubApiClient githubApiClient,
                         GithubCredentials githubCredentials, long fileSizeLimit) {
        client = httpClient;
        authClient = githubAuthClient;
        apiClient = githubApiClient;
        credentials = githubCredentials;
        sizeLimit = fileSizeLimit;
    }

    @Nonnull
    @Override
    public Pair<String, String> loginApplication(String code, String state) {
        Response<JsonObject> rs = WebClientCommon.executeServiceCall(authClient.loginApplication(credentials.getId(),
                credentials.getToken(), code, state, null));
        if (rs.body() == null) {
            throw new ConnectionException("Unable to connect to Github API");
        }
        JsonObject body = rs.body();
        if (!body.has(ACCESS_TOKEN) || !body.has(ACCESS_TOKEN_TYPE)) {
            throw INVALID_API_RESPONSE;
        }
        JsonElement tokenObject = body.get(ACCESS_TOKEN);
        JsonElement tokenTypeObject = body.get(ACCESS_TOKEN_TYPE);
        if (tokenObject.isJsonNull() || tokenTypeObject.isJsonNull()) {
            throw INVALID_API_RESPONSE;
        }
        String githubToken = tokenObject.getAsString();
        String tokenType = tokenTypeObject.getAsString();
        return Pair.of(tokenType, githubToken);
    }

    @Nonnull
    private JsonObject getFileInfo(String authHeader, String repo, String branch, String filePath) {
        String directoryPath;
        String fileName;
        if (filePath.contains(DIRECTORY_DELIMITER)) {
            int delimiterIndex = filePath.lastIndexOf(DIRECTORY_DELIMITER);
            directoryPath = filePath.substring(0, delimiterIndex);
            fileName = filePath.substring(delimiterIndex + 1);
        } else {
            directoryPath = "";
            fileName = filePath;
        }


        Response<JsonElement> mergeFileDirectoryInfoRs =
                WebClientCommon.executeServiceCall(apiClient.getContent(authHeader, repo, directoryPath, branch));
        JsonElement mergeFileDirectoryInfo = mergeFileDirectoryInfoRs.body();

        if (mergeFileDirectoryInfo == null || !mergeFileDirectoryInfo.isJsonArray()) {
            throw UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE;
        }

        JsonArray directory = mergeFileDirectoryInfo.getAsJsonArray();
        for (JsonElement e : directory) {
            if (!e.isJsonObject()) {
                throw UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE;
            }
            JsonObject element = e.getAsJsonObject();
            if (!element.has(TYPE_FIELD) || !element.getAsJsonPrimitive(TYPE_FIELD).isString()) {
                throw UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE;
            }
            if (!"file".equals(element.getAsJsonPrimitive(TYPE_FIELD).getAsString())) {
                continue;
            }
            if (!element.has(NAME_FIELD) || !element.getAsJsonPrimitive(NAME_FIELD).isString()) {
                throw UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE;
            }
            if (fileName.equals(element.getAsJsonPrimitive(NAME_FIELD).getAsString())) {
                return element;
            }
        }
        throw UNABLE_TO_GET_CONFIGURATION_EXCEPTION_NO_FILE;
    }

    @Nonnull
    @Override
    public byte[] getFileContent(String authHeader, String repo, String branch, String filePath) {
        JsonObject mergeFileInfo = getFileInfo(authHeader, repo, branch, filePath);
        if (!mergeFileInfo.has(SIZE_FIELD) || !mergeFileInfo.getAsJsonPrimitive(SIZE_FIELD).isNumber()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }
        long size = mergeFileInfo.getAsJsonPrimitive(SIZE_FIELD).getAsLong();

        if (size > sizeLimit) {
            throw UNABLE_TO_GET_CONFIGURATION_FILE_TOO_BIG;
        }

        JsonElement fileElement = WebClientCommon.executeServiceCall(apiClient.getContent(authHeader, repo, filePath, branch)).body();
        if (fileElement == null || !fileElement.isJsonObject()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }

        JsonObject file = fileElement.getAsJsonObject();
        if (!file.has(CONTENT_FIELD) || !file.get(CONTENT_FIELD).isJsonPrimitive() || !file.getAsJsonPrimitive(CONTENT_FIELD).isString()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }

        return Base64.getDecoder()
                .decode(file.getAsJsonPrimitive(CONTENT_FIELD).getAsString().replace("\n", "").replace("\r", ""));
    }
}
