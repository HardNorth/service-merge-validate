package net.hardnorth.github.merge.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.*;

public interface GithubApiClient {

    @POST("app/installations/{installation_id}/access_tokens")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> authenticateInstallation(@Header("Authorization") String auth,
                                              @Path("installation_id") long installationId);

    @GET("repos/{owner}/{repo}/contents/{path}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonElement> getContent(@Header("Authorization") String auth, @Path("owner") String owner,
                                 @Path("repo") String repo, @Path("path") String path, @Query("ref") String ref);

    @GET("repos/{owner}/{repo}/branches/{branch}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getBranch(@Header("Authorization") String auth, @Path("owner") String owner,
                               @Path("repo") String repo, @Path("branch") String branch);

    @GET("repos/{owner}/{repo}/branches/{branch}/protection")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getBranchProtection(@Header("Authorization") String auth, @Path("owner") String owner,
                                         @Path("repo") String repo, @Path("branch") String branch);

    @GET("repos/{user}/{repo}/compare/{base}...{head}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> compareCommits(@Header("Authorization") String auth, @Path("user") String user,
                                    @Path("repo") String repo, @Path("base") String base, @Path("head") String head);

    @POST("repos/{owner}/{repo}/merges")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> mergeBranches(@Header("Authorization") String auth, @Path("owner") String owner,
                                   @Path("repo") String repo, @Body JsonObject body);

    // Pull requests

    @POST("repos/{owner}/{repo}/pulls")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> createPullRequest(@Header("Authorization") String auth, @Path("owner") String owner,
                                       @Path("repo") String repo, @Body JsonObject body);

    @GET("repos/{owner}/{repo}/pulls/{pull_number}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getPullRequest(@Header("Authorization") String auth, @Path("owner") String owner,
                                    @Path("repo") String repo, @Path("pull_number") int pullNumber);

    @GET("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> checkIfPullRequestMerged(@Header("Authorization") String auth, @Path("owner") String owner,
                                              @Path("repo") String repo, @Path("pull_number") int pullNumber);

    @POST("repos/{owner}/{repo}/pulls/{pull_number}/reviews")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> createReview(@Header("Authorization") String auth, @Path("owner") String owner,
                                  @Path("repo") String repo, @Path("pull_number") int pullNumber,
                                  @Body JsonObject body);

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> mergePullRequest(@Header("Authorization") String auth, @Path("owner") String owner,
                                      @Path("repo") String repo, @Path("pull_number") int pullNumber,
                                      @Body JsonObject body);
}
