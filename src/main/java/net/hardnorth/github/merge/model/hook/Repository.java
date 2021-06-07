package net.hardnorth.github.merge.model.hook;

import com.google.gson.annotations.SerializedName;

public class Repository {

    private String name;

    @SerializedName("full_name")
    private String fullName;

    private User owner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
