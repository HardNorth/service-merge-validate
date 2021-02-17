package net.hardnorth.github.merge.service.impl;

import com.google.gson.JsonObject;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.GithubApiClient;
import net.hardnorth.github.merge.service.MergeValidate;
import net.hardnorth.github.merge.utils.WebClientCommon;
import org.apache.http.HttpStatus;

import java.util.Base64;

public class MergeValidateService implements MergeValidate {

    private static final String TYPE_FIELD = "type";
    private static final String CONTENT_FIELD = "content";

    private final GithubApiClient client;
    private final String mergeFile;
    private final java.nio.charset.Charset charset;

    public MergeValidateService(GithubApiClient githubClient, String mergeFileName, Charset currentCharset) {
        client = githubClient;
        mergeFile = mergeFileName;
        charset = currentCharset.getValue();
    }

    @Override
    public void merge(String authToken, String repo, String from, String to) {
        JsonObject mergeFileInfo
                = WebClientCommon.executeServiceCall(client.getContent(authToken, repo, mergeFile, to)).body();

        if (mergeFileInfo == null) {
            throw new HttpException("Unable to get merge configuration for target branch",
                    HttpStatus.SC_FAILED_DEPENDENCY);
        }


        if (!mergeFileInfo.has(TYPE_FIELD) || !mergeFileInfo.getAsJsonPrimitive(TYPE_FIELD).isString()
                || !"file".equals(mergeFileInfo.getAsJsonPrimitive(TYPE_FIELD).getAsString())
                || !mergeFileInfo.has(CONTENT_FIELD) || !mergeFileInfo.getAsJsonPrimitive(CONTENT_FIELD).isString()) {
            throw new IllegalArgumentException("Invalid merge configuration file");
        }

        String mergeFileContent =
                new String(Base64.getDecoder().decode(mergeFileInfo.getAsJsonPrimitive(CONTENT_FIELD).getAsString()), charset);

    }
}
