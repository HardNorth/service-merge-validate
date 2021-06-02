package net.hardnorth.github.merge.model.hook;

public class PushRequest {

    private Installation installation;

    private boolean created;

    public Installation getInstallation() {
        return installation;
    }

    public void setInstallation(Installation installation) {
        this.installation = installation;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }
}
