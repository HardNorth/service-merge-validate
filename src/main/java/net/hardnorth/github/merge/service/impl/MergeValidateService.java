package net.hardnorth.github.merge.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.GithubApiClient;
import net.hardnorth.github.merge.service.MergeValidate;
import net.hardnorth.github.merge.utils.WebClientCommon;
import org.apache.http.HttpStatus;
import retrofit2.Response;

import javax.annotation.Nonnull;
import java.util.Base64;
import java.util.logging.Logger;

public class MergeValidateService implements MergeValidate {

    private static final Logger LOGGER = Logger.getLogger(MergeValidateService.class.getSimpleName());

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_INVALID_RESPONSE
            = new HttpException("Unable to get merge configuration for target branch: invalid response", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_LIMIT_EXCEED
            = new HttpException("Unable to get merge configuration for target branch: scan limit exceed", HttpStatus.SC_REQUEST_TOO_LONG);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_EXCEPTION_NO_FILE
            = new HttpException("Unable to get merge configuration for target branch: no configuration file found", HttpStatus.SC_BAD_REQUEST);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON
            = new HttpException("Unable to get merge configuration for target branch: response is not JSON", HttpStatus.SC_FAILED_DEPENDENCY);

    private static final RuntimeException UNABLE_TO_GET_CONFIGURATION_FILE_TOO_BIG
            = new HttpException("Unable to get merge configuration for target branch: file size limit exceed", HttpStatus.SC_REQUEST_TOO_LONG);

    private static final String DIRECTORY_DELIMITER = "/";
    private static final String TYPE_FIELD = "type";
    private static final String NAME_FIELD = "name";
    private static final String CONTENT_FIELD = "content";
    private static final String SIZE_FIELD = "size";

    private final GithubApiClient client;
    private final String mergeFile;
    private final String mergeFileDirectory;
    private final java.nio.charset.Charset charset;
    private final long sizeLimit;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateService(GithubApiClient githubClient, String mergeFileName, Charset currentCharset,
                                long fileSizeLimit) {
        client = githubClient;
        mergeFile = mergeFileName;
        charset = currentCharset.getValue();
        mergeFileDirectory = mergeFile.contains(DIRECTORY_DELIMITER) ? mergeFile.substring(0, mergeFile.lastIndexOf(DIRECTORY_DELIMITER)) : "";
        sizeLimit = fileSizeLimit;
    }

    @Nonnull
    private JsonObject getMergeFileInfo(String authHeader, String repo, String branch) {
        Response<JsonElement> mergeFileDirectoryInfoRs =
                WebClientCommon.executeServiceCall(client.getContent(authHeader, repo, mergeFileDirectory, branch));
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
            if (mergeFile.equals(element.getAsJsonPrimitive(NAME_FIELD).getAsString())) {
                return element;
            }
        }
        throw UNABLE_TO_GET_CONFIGURATION_EXCEPTION_NO_FILE;
    }

    private String getMergeFileContent(String authHeader, String repo, String branch) {
        JsonObject mergeFileInfo = getMergeFileInfo(authHeader, repo, branch);
        if (!mergeFileInfo.has(SIZE_FIELD) || !mergeFileInfo.getAsJsonPrimitive(SIZE_FIELD).isNumber()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }
        long size = mergeFileInfo.getAsJsonPrimitive(SIZE_FIELD).getAsLong();

        if (size > sizeLimit) {
            throw UNABLE_TO_GET_CONFIGURATION_FILE_TOO_BIG;
        }

        JsonElement fileElement = WebClientCommon.executeServiceCall(client.getContent(authHeader, repo, mergeFile, branch)).body();
        if (fileElement == null || !fileElement.isJsonObject()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }

        JsonObject file = fileElement.getAsJsonObject();
        if (!file.has(CONTENT_FIELD) || !file.get(CONTENT_FIELD).isJsonPrimitive() || !file.getAsJsonPrimitive(CONTENT_FIELD).isString()) {
            throw UNABLE_TO_GET_CONFIGURATION_RESPONSE_IS_NOT_JSON;
        }

        return new String(Base64.getDecoder().decode(file.getAsJsonPrimitive(CONTENT_FIELD).getAsString()), charset);
    }

    @Override
    public void merge(String authHeader, String repo, String from, String to) {
        String mergeFileContent = getMergeFileContent(authHeader, repo, to);
        LOGGER.finer("Got merge file: " + mergeFileContent);

    }
}
