package net.hardnorth.github.merge.service;

import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface GithubApiClient {

    @GET("repos/{repo}/pulls")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getPullRequestList(@Header("Authorization") String auth, @Path("repo") String repo,
                                        @QueryMap Map<String, String> params);

    @POST("repos/{repo}/pulls")
    @FormUrlEncoded
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> createPullRequest(@Header("Authorization") String auth, @Path("repo") String repo,
                                       @FieldMap Map<String, String> params);


    @GET("repos/{repo}/pulls/{number}")
    @Headers(HttpHeaders.ACCEPT + ": application/vnd.github.v3+json")
    Call<JsonObject> getPullRequest(@Header("Authorization") String auth, @Path("repo") String repo,
                                    @Path("number") int number);

}
