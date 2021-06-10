package net.hardnorth.github.merge.model.repo;

import com.google.gson.annotations.SerializedName;

public class BranchProtection {

    @SerializedName("required_status_checks")
    private RequiredStatusChecks requiredStatusChecks;

    @SerializedName("required_pull_request_reviews")
    private RequiredPullRequestReviews requiredPullRequestReviews;

    public RequiredStatusChecks getRequiredStatusChecks() {
        return requiredStatusChecks;
    }

    public void setRequiredStatusChecks(RequiredStatusChecks requiredStatusChecks) {
        this.requiredStatusChecks = requiredStatusChecks;
    }

    public RequiredPullRequestReviews getRequiredPullRequestReviews() {
        return requiredPullRequestReviews;
    }

    public void setRequiredPullRequestReviews(RequiredPullRequestReviews requiredPullRequestReviews) {
        this.requiredPullRequestReviews = requiredPullRequestReviews;
    }
}
