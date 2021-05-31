package net.hardnorth.github.merge.model.hook;

import com.google.gson.annotations.SerializedName;

public class Repository {

    @SerializedName("full_name")
    private String fullName;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
