package net.hardnorth.github.merge.model.github.hook;

public class PullRequest {
    private Branch base;

    private Branch head;

    public Branch getBase() {
        return base;
    }

    public void setBase(Branch base) {
        this.base = base;
    }

    public Branch getHead() {
        return head;
    }

    public void setHead(Branch head) {
        this.head = head;
    }
}
