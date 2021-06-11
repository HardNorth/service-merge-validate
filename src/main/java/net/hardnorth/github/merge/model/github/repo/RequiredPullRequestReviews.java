package net.hardnorth.github.merge.model.github.repo;

import com.google.gson.annotations.SerializedName;

public class RequiredPullRequestReviews {
    @SerializedName("dismiss_stale_reviews")
    private Boolean dismissStaleReviews;

    @SerializedName("require_code_owner_reviews")
    private Boolean requireCodeOwnerReviews;

    public Boolean getDismissStaleReviews() {
        return dismissStaleReviews;
    }

    public void setDismissStaleReviews(Boolean dismissStaleReviews) {
        this.dismissStaleReviews = dismissStaleReviews;
    }

    public Boolean getRequireCodeOwnerReviews() {
        return requireCodeOwnerReviews;
    }

    public void setRequireCodeOwnerReviews(Boolean requireCodeOwnerReviews) {
        this.requireCodeOwnerReviews = requireCodeOwnerReviews;
    }
}
