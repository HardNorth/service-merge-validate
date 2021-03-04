package net.hardnorth.github.merge.model;

import java.util.ArrayList;
import java.util.List;

public class CommitDifference {

    private final int aheadBy;
    private final int behindBy;
    private final List<FileChange> commits;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public CommitDifference(int aheadByCount, int behindByCount, List<FileChange> commitDifference) {
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

    public List<FileChange> getCommits() {
        return new ArrayList<>(commits);
    }

    @Override
    public String toString() {
        return "[Ahead: " + aheadBy + "; Behind: " + behindBy + "; Changes: " + commits + "]";
    }
}
