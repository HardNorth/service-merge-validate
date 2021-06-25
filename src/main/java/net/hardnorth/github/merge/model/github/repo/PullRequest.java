package net.hardnorth.github.merge.model.github.repo;

import com.google.gson.annotations.SerializedName;

public class PullRequest {
    private Integer number;

    private Boolean mergeable;

    @SerializedName("mergeable_state")
    private String mergeableState;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Boolean getMergeable() {
        return mergeable;
    }

    public void setMergeable(Boolean mergeable) {
        this.mergeable = mergeable;
    }

    public String getMergeableState() {
        return mergeableState;
    }

    public void setMergeableState(String mergeableState) {
        this.mergeableState = mergeableState;
    }
}
