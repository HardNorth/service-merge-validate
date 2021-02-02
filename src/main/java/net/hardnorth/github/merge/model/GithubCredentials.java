package net.hardnorth.github.merge.model;

public class GithubCredentials {

    private final String id;

    private final String token;

    public GithubCredentials(String clientId, String clientToken){
        id = clientId;
        token = clientToken;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
}
