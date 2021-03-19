package net.hardnorth.github.merge.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.*;

public interface GithubApiClient {

    @GET("repos/{user}/{repo}/contents/{path}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonElement> getContent(@Header("Authorization") String auth, @Path("user") String user,
                                 @Path("repo") String repo, @Path("path") String path, @Query("ref") String ref);

    @GET("/repos/{user}/{repo}/branches/{branch}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getBranch(@Header("Authorization") String auth, @Path("user") String user,
                               @Path("repo") String repo, @Path("branch") String branch);

    @GET("/repos/{user}/{repo}/compare/{base}...{head}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> compareCommits(@Header("Authorization") String auth, @Path("user") String user,
                                    @Path("repo") String repo, @Path("base") String base, @Path("head") String head);

    @POST("/repos/{user}/{repo}/merges")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> mergeBranches(@Header("Authorization") String auth, @Path("user") String user,
                                   @Path("repo") String repo, @Body JsonObject body);
}
