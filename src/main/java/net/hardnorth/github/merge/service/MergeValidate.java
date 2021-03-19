package net.hardnorth.github.merge.service;

public interface MergeValidate {

    void merge(String authToken, String user, String repo, String from, String to);

}
