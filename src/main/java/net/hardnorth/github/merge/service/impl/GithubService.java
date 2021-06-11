package net.hardnorth.github.merge.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.CommitDifference;
import net.hardnorth.github.merge.model.FileChange;
import net.hardnorth.github.merge.model.github.repo.BranchProtection;
import net.hardnorth.github.merge.model.github.repo.PullRequest;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.GithubApiClient;
import net.hardnorth.github.merge.utils.WebServiceCommon;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import retrofit2.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static net.hardnorth.github.merge.utils.WebClientCommon.executeServiceCall;

public class GithubService implements Github {
    private static final Logger LOGGER = Logger.getLogger(GithubService.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final String DIRECTORY_DELIMITER = "/";
    private static final String TOKEN = "token";
    private static final String EXPIRES_AT = "expires_at";
    private static final String TYPE_FIELD = "type";
    private static final String NAME_FIELD = "name";
    private static final String CONTENT_FIELD = "content";
    private static final String SIZE_FIELD = "size";
    private static final String COMMIT_FIELD = "commit";
    private static final String COMMIT_HASH_FIELD = "sha";
    private static final String AHEAD_BY_FIELD = "ahead_by";
    private static final String BEHIND_BY_FIELD = "behind_by";
    private static final String FILES_FIELD = "files";
    private static final String FILENAME_FIELD = "filename";
    private static final String STATUS_FIELD = "status";
    private static final String BASE_FIELD = "base";
    private static final String HEAD_FIELD = "head";
    private static final String TITLE_FIELD = "title";
    private static final String BODY_FIELD = "body";
    private static final String EVENT_FIELD = "event";
    private static final String COMMIT_TITLE_FIELD = "commit_title";
    private static final String COMMIT_MESSAGE_FIELD = "commit_message";
    private static final String MERGE_METHOD_FIELD = "merge_method";
    private static final String NUMBER_FIELD = "number";
    private static final String CONTEXTS_FIELD = "contexts";


    public static final RuntimeException INVALID_API_RESPONSE = new ConnectionException("Invalid response from Github API");
    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE
            = new HttpException("Unable to get merge configuration for target branch: invalid response", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_PARSE_EXPIRE_DATE
            = new HttpException("Unable to parse token expiration date", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_NO_FILE
            = new HttpException("Unable to get merge configuration for target branch: no configuration file found", HttpStatus.SC_BAD_REQUEST);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON
            = new HttpException("Unable to get merge configuration for target branch: response is not JSON", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_FILE_TOO_BIG
            = new HttpException("Unable to get merge configuration for target branch: file size limit exceed", HttpStatus.SC_REQUEST_TOO_LONG);

    private static final RuntimeException UNABLE_TO_GET_BRANCH_RESPONSE_IS_NOT_JSON
            = new HttpException("Unable to get information for target branch: response is not JSON", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_COMPARE_COMMITS_RESPONSE_IS_NOT_JSON
            = new HttpException("Unable to compare commits: response is not JSON", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_COMPARE_COMMITS_INVALID_FILES_FORMAT
            = new HttpException("Unable to compare commits: invalid files format", HttpStatus.SC_FAILED_DEPENDENCY);

    private final GithubApiClient apiClient;
    private final long sizeLimit;
    private final java.nio.charset.Charset charset;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubService(GithubApiClient githubApiClient, long fileSizeLimit, Charset configuredCharset) {
        apiClient = githubApiClient;
        sizeLimit = fileSizeLimit;
        charset = configuredCharset.get();
    }

    @Nonnull
    @Override
    public Pair<String, Date> authenticateInstallation(@Nullable String authHeader, long installationId) {
        Response<JsonObject> response =
                executeServiceCall(apiClient.authenticateInstallation(authHeader, installationId), charset);
        JsonObject tokenObject = response.body();
        if (tokenObject == null) {
            LOGGER.warnf("Invalid installation authentication response: no body");
            throw INVALID_API_RESPONSE;
        }
        if (!tokenObject.has(TOKEN) || !tokenObject.has(EXPIRES_AT)) {
            LOGGER.warnf("Invalid installation authentication response: has token - %b; has expiration - %b",
                    tokenObject.has(TOKEN), tokenObject.has(EXPIRES_AT));
            throw INVALID_API_RESPONSE;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        try {
            return Pair.of(tokenObject.get(TOKEN).getAsString(), sdf.parse(tokenObject.get(EXPIRES_AT).getAsString()));
        } catch (ParseException e) {
            throw UNABLE_TO_PARSE_EXPIRE_DATE;
        }
    }

    @Nonnull
    private JsonObject getFileInfo(String authHeader, String user, String repo, String branch, String filePath) {
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
                executeServiceCall(apiClient.getContent(authHeader, user, repo, directoryPath, branch), charset);
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
    public byte[] getFileContent(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                                 @Nullable String branch, @Nonnull String filePath) {
        JsonObject fileInfo = getFileInfo(authHeader, user, repo, branch, filePath);
        if (!fileInfo.has(SIZE_FIELD) || !fileInfo.getAsJsonPrimitive(SIZE_FIELD).isNumber()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }
        long size = fileInfo.getAsJsonPrimitive(SIZE_FIELD).getAsLong();

        if (size > sizeLimit) {
            throw UNABLE_TO_GET_CONFIGURATION_FILE_TOO_BIG;
        }

        JsonElement fileElement = executeServiceCall(apiClient.getContent(authHeader, user, repo, filePath, branch), charset).body();
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

    @Nonnull
    @Override
    public String getLatestCommit(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                                  @Nullable String branch) {
        JsonObject branchInfo = executeServiceCall(apiClient.getBranch(authHeader, user, repo, branch), charset).body();
        if (branchInfo == null || !branchInfo.has(COMMIT_FIELD) || !branchInfo.get(COMMIT_FIELD).isJsonObject()) {
            throw UNABLE_TO_GET_BRANCH_RESPONSE_IS_NOT_JSON;
        }
        JsonObject commitInfo = branchInfo.getAsJsonObject(COMMIT_FIELD);
        if (commitInfo == null || !commitInfo.has(COMMIT_HASH_FIELD) || !commitInfo.get(COMMIT_HASH_FIELD).isJsonPrimitive()
                || !commitInfo.getAsJsonPrimitive(COMMIT_HASH_FIELD).isString()) {
            throw UNABLE_TO_GET_BRANCH_RESPONSE_IS_NOT_JSON;
        }
        return commitInfo.getAsJsonPrimitive(COMMIT_HASH_FIELD).getAsString();
    }

    @Nonnull
    @Override
    public CommitDifference listChanges(@Nullable String authHeader, @Nullable String user, @Nullable String repo,
                                        @Nullable String source, @Nullable String dest) {
        String sourceCommit = getLatestCommit(authHeader, user, repo, source);
        String destCommit = getLatestCommit(authHeader, user, repo, dest);

        JsonObject diff = executeServiceCall(apiClient.compareCommits(authHeader, user, repo, destCommit, sourceCommit), charset).body();
        if (diff == null || !diff.has(AHEAD_BY_FIELD) || !diff.get(AHEAD_BY_FIELD).isJsonPrimitive() || !diff.getAsJsonPrimitive(AHEAD_BY_FIELD).isNumber()
                || !diff.has(BEHIND_BY_FIELD) || !diff.get(BEHIND_BY_FIELD).isJsonPrimitive() || !diff.getAsJsonPrimitive(BEHIND_BY_FIELD).isNumber()
                || !diff.has(FILES_FIELD) || !diff.get(FILES_FIELD).isJsonArray()) {
            throw UNABLE_TO_COMPARE_COMMITS_RESPONSE_IS_NOT_JSON;
        }

        JsonArray commits = diff.getAsJsonArray(FILES_FIELD);
        List<FileChange> fileChanges = StreamSupport.stream(commits.spliterator(), false).map(f -> {
            if (!f.isJsonObject()) {
                throw UNABLE_TO_COMPARE_COMMITS_INVALID_FILES_FORMAT;
            }
            JsonObject fo = f.getAsJsonObject();
            if (!fo.has(STATUS_FIELD) || !fo.has(FILENAME_FIELD) || !fo.get(STATUS_FIELD).isJsonPrimitive()
                    || !fo.get(FILENAME_FIELD).isJsonPrimitive() || !fo.getAsJsonPrimitive(STATUS_FIELD).isString()
                    || !fo.getAsJsonPrimitive(FILENAME_FIELD).isString()) {
                throw UNABLE_TO_COMPARE_COMMITS_INVALID_FILES_FORMAT;
            }
            FileChange.Type type = FileChange.Type.getByStatus(fo.getAsJsonPrimitive(STATUS_FIELD).getAsString());
            if (type == null) {
                throw UNABLE_TO_COMPARE_COMMITS_INVALID_FILES_FORMAT;
            }
            return new FileChange(type, fo.getAsJsonPrimitive(FILENAME_FIELD).getAsString());
        }).collect(Collectors.toList());

        return new CommitDifference(diff.getAsJsonPrimitive(AHEAD_BY_FIELD).getAsInt(),
                diff.getAsJsonPrimitive(BEHIND_BY_FIELD).getAsInt(),
                fileChanges);
    }

    @Override
    public void merge(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                      @Nullable String source, @Nullable String dest, @Nullable String message) {
        JsonObject request = new JsonObject();
        ofNullable(dest).ifPresent(d -> request.add(BASE_FIELD, new JsonPrimitive(d)));
        ofNullable(source).ifPresent(s -> request.add(HEAD_FIELD, new JsonPrimitive(s)));
        ofNullable(message).ifPresent(m -> request.add(COMMIT_MESSAGE_FIELD, new JsonPrimitive(m)));
        executeServiceCall(apiClient.mergeBranches(authHeader, owner, repo, request), charset);
    }

    @Override
    public int createPullRequest(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                                 @Nullable String source, @Nullable String dest, @Nullable String title,
                                 @Nullable String body) {
        JsonObject request = new JsonObject();
        ofNullable(dest).ifPresent(d -> request.add(BASE_FIELD, new JsonPrimitive(d)));
        ofNullable(source).ifPresent(s -> request.add(HEAD_FIELD, new JsonPrimitive(s)));
        ofNullable(title).ifPresent(m -> request.add(TITLE_FIELD, new JsonPrimitive(m)));
        ofNullable(body).ifPresent(m -> request.add(BODY_FIELD, new JsonPrimitive(m)));
        Response<JsonObject> result =
                executeServiceCall(apiClient.createPullRequest(authHeader, owner, repo, request), charset);
        return ofNullable(result.body()).map(b -> b.getAsJsonPrimitive(NUMBER_FIELD)).map(JsonPrimitive::getAsInt)
                .orElseThrow(() -> INVALID_API_RESPONSE);
    }

    @Override
    public void createReview(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                             int pullNumber, @Nullable String event, @Nullable String body) {
        JsonObject request = new JsonObject();
        ofNullable(event).ifPresent(m -> request.add(EVENT_FIELD, new JsonPrimitive(m)));
        ofNullable(body).ifPresent(m -> request.add(BODY_FIELD, new JsonPrimitive(m)));
        executeServiceCall(apiClient.createReview(authHeader, owner, repo, pullNumber, request), charset);
    }

    @Override
    public void mergePullRequest(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                                 int pullNumber, @Nullable String commitTitle, @Nullable String commitMessage,
                                 @Nullable String mergeMethod) {
        JsonObject request = new JsonObject();
        ofNullable(commitTitle).ifPresent(m -> request.add(COMMIT_TITLE_FIELD, new JsonPrimitive(m)));
        ofNullable(commitMessage).ifPresent(m -> request.add(COMMIT_MESSAGE_FIELD, new JsonPrimitive(m)));
        ofNullable(mergeMethod).ifPresent(m -> request.add(MERGE_METHOD_FIELD, new JsonPrimitive(m)));
        executeServiceCall(apiClient.mergePullRequest(authHeader, owner, repo, pullNumber, request), charset);
    }

    @Override
    public BranchProtection getBranchProtection(@Nullable String authHeader, @Nullable String owner, @Nullable String repo,
                                                @Nullable String branch) {
        Response<JsonObject> response =
                executeServiceCall(apiClient.getBranchProtection(authHeader, owner, repo, branch), charset);
        return ofNullable(response.body())
                .map(b -> WebServiceCommon.deserializeJson(b, BranchProtection.class))
                .orElseThrow(() -> INVALID_API_RESPONSE);
    }

    @Override
    public List<PullRequest> getOpenedPullRequests(@Nullable String authHeader, @Nullable String owner,
                                                   @Nullable String repo, @Nullable String branch) {
        Response<JsonArray> response =
                executeServiceCall(apiClient.getPullRequests(authHeader, owner, repo, "open", null, branch,
                        null, null, null, null), charset);
        return ofNullable(response.body())
                .map(b -> WebServiceCommon.<List<PullRequest>>deserializeJson(b, new TypeToken<List<PullRequest>>() {
                }.getType()))
                .orElseThrow(() -> INVALID_API_RESPONSE);
    }
}
