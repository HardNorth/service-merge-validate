package net.hardnorth.github.merge.service;

import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import javax.ws.rs.core.MediaType;

public interface GithubClient {

    @POST("login/oauth/access_token")
    @FormUrlEncoded
    @Headers(HttpHeaders.ACCEPT + ": " + MediaType.APPLICATION_JSON)
    Call<JsonObject> loginApplication(@Field("client_id") String clientId, @Field("client_secret") String clientSecret,
                                      @Field("code") String code, @Field("state") String state,
                                      @Field("redirect_uri") String redirectUri);

}
