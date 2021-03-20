package net.hardnorth.github.merge.service.impl;

import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.CommitDifference;
import net.hardnorth.github.merge.model.FileChange;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.MergeValidate;
import net.hardnorth.github.merge.utils.ValidationPattern;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MergeValidateService implements MergeValidate {


    private static final RuntimeException NOT_FAST_FORWARD = new IllegalArgumentException("Unable to merge branches: not fast forward");
    private static final RuntimeException ILLEGAL_CHANGES = new IllegalArgumentException("Unable to merge branches: illegal changes");

    private final Github client;
    private final String mergeFile;
    private final java.nio.charset.Charset charset;
    private final List<String> strictRules;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public MergeValidateService(@Nonnull Github githubClient, @Nonnull String mergeFileName,
                                @Nonnull Charset currentCharset) {
        client = githubClient;
        mergeFile = mergeFileName;
        charset = currentCharset.getValue();
        strictRules = Arrays.asList("!" + mergeFile, "!.github/workflows/**");
    }

    @Override
    public void merge(String authHeader, String user, String repo, String from, String to) {
        String mergeFileContent = new String(client.getFileContent(authHeader, user, repo, to, mergeFile), charset);
        ValidationPattern pattern = ValidationPattern.parse(mergeFileContent);
        strictRules.forEach(pattern::addRule);

        CommitDifference difference = client.listChanges(authHeader, user, repo, from, to);
        if (difference.getBehindBy() > 0) {
            throw NOT_FAST_FORWARD;
        }
        if (difference.getAheadBy() <= 0 || difference.getCommits().isEmpty()) {
            return; // nothing to merge
        }

        boolean illegalChanges = difference.getCommits().stream()
                .anyMatch(c -> FileChange.Type.ADDED == c.getType() || FileChange.Type.DELETED == c.getType());
        if (illegalChanges) {
            throw ILLEGAL_CHANGES;
        }
        boolean allConform = difference.getCommits().stream().allMatch(c -> pattern.test(new File(c.getName()).toPath()));
        if (!allConform) {
            throw ILLEGAL_CHANGES;
        }

        client.merge(authHeader, user, repo, from, to, String.format("Merge branch %s into %s", from, to));
    }
}
