package net.hardnorth.github.merge.service;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface GithubClientApi {

    @POST("login/oauth/access_token")
    @FormUrlEncoded
    Call<JsonObject> loginApplication(@Field("client_id") String clientId, @Field("client_secret") String clientSecret,
                                      @Field("code") String code, @Field("state") String state,
                                      @Field("redirect_uri") String redirectUri);

}
