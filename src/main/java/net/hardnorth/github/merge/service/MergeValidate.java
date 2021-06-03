package net.hardnorth.github.merge.service;

public interface MergeValidate {

    void merge(String authToken, String owner, String repo, String from, String to);

}
