package net.hardnorth.github.merge.model;

import java.util.ArrayList;
import java.util.List;

public class CommitDifference {

    private final int aheadBy;
    private final int behindBy;
    private final List<Change> commits;

    public CommitDifference(int aheadByCount, int behindByCount, List<Change> commitDifference) {
        aheadBy = aheadByCount;
        behindBy = behindByCount;
        commits = new ArrayList<>(commitDifference);
    }

    public int getAheadBy() {
        return aheadBy;
    }

    public int getBehindBy() {
        return behindBy;
    }

    public List<Change> getCommits() {
        return new ArrayList<>(commits);
    }
}
