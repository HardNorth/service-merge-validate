package net.hardnorth.github.merge.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class FileChange {
    public enum Type {
        ADDED("added"), CHANGED("modified"), DELETED("deleted");

        private final String status;

        Type(String githubStatus) {
            status = githubStatus;
        }

        @Nullable
        public static Type getByStatus(final String githubStatus) {
            return Arrays.stream(values()).filter(v -> v.status.equals(githubStatus)).findAny().orElse(null);
        }
    }

    private final Type type;
    private final String name;

    public FileChange(@Nonnull final Type changeType, @Nonnull final String changedFileName) {
        type = changeType;
        name = changedFileName;
    }

    @Nonnull
    public Type getType() {
        return type;
    }

    @Nonnull
    public String getName() {
        return name;
    }
}
