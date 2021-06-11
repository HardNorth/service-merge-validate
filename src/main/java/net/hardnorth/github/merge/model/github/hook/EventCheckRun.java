package net.hardnorth.github.merge.model.github.hook;

import com.google.gson.annotations.SerializedName;

public class EventCheckRun {
    private String action;

    @SerializedName("check_run")
    private CheckRun checkRun;

    private Repository repository;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public CheckRun getCheckRun() {
        return checkRun;
    }

    public void setCheckRun(CheckRun checkRun) {
        this.checkRun = checkRun;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
