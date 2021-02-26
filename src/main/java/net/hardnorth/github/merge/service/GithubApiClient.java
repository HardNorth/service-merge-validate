package net.hardnorth.github.merge.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.*;

import javax.ws.rs.QueryParam;
import java.util.Map;

public interface GithubApiClient {

    @GET("repos/{repo}/contents/{path}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonElement> getContent(@Header("Authorization") String auth, @Path("repo") String repo,
                                 @Path("path") String path, @QueryParam("ref") String ref);

    @GET("/repos/{repo}/branches/{branch}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getBranch(@Header("Authorization") String auth, @Path("repo") String repo,
                                 @Path("branch") String branch);

    @GET("/repos/{repo}/branches/{branch}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> compareCommits(@Header("Authorization") String auth, @Path("repo") String repo,
                               @Path("branch") String branch);
}
