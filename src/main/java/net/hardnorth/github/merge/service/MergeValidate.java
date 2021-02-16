package net.hardnorth.github.merge.service;

public interface MergeValidate {

    void merge(String authToken, String repo, String from, String to);

}
