package net.hardnorth.github.merge.model.hook;

import com.google.gson.annotations.SerializedName;

public class Repository {

    private String name;

    @SerializedName("full_name")
    private String fullName;

    private Owner owner;

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

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }
}
