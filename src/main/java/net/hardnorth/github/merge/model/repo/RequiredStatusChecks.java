package net.hardnorth.github.merge.model.repo;

import java.util.List;

public class RequiredStatusChecks {

    private Boolean strict;
    private List<String> contexts;

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public void setContexts(List<String> contexts) {
        this.contexts = contexts;
    }
}
