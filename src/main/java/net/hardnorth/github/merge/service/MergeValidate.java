package net.hardnorth.github.merge.service;

public interface MergeValidate {

    void validate(String authHeader, String user, String repo, String from, String to);

}
