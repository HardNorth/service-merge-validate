package net.hardnorth.github.merge.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.exception.HttpException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.GithubApiClient;
import net.hardnorth.github.merge.service.MergeValidate;
import net.hardnorth.github.merge.utils.ValidationPattern;
import net.hardnorth.github.merge.utils.WebClientCommon;
import org.apache.http.HttpStatus;
import retrofit2.Response;

import javax.annotation.Nonnull;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class MergeValidateService implements MergeValidate {

    private final Github client;
    private final String mergeFile;
    private final java.nio.charset.Charset charset;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateService(Github githubClient, String mergeFileName, Charset currentCharset) {
        client = githubClient;
        mergeFile = mergeFileName;
        charset = currentCharset.getValue();
    }

    private List<String> getChangedFiles(String authHeader, String repo, String from, String to) {
        return Collections.emptyList();
    }

    @Override
    public void merge(String authHeader, String repo, String from, String to) {
        String mergeFileContent = new String(client.getFileContent(authHeader, repo, to, mergeFile), charset);
        ValidationPattern pattern = ValidationPattern.parse(mergeFileContent);


    }
}
