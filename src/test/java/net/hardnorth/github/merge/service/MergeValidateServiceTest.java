package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.CommitDifference;
import net.hardnorth.github.merge.model.FileChange;
import net.hardnorth.github.merge.service.impl.MergeValidateService;
import net.hardnorth.github.merge.utils.IoUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MergeValidateServiceTest {
    private static final String MERGE_FILE_NAME = ".merge-validate";
    private static final String AUTHORIZATION = "auth";
    private static final String USER = "HardNorth";
    private static final String REPO = "test";
    private static final String SOURCE_BRANCH = "source";
    private static final String DEST_BRANCH = "dest";
    private static final String WORKFLOW_FILE = ".github/workflows/ci.yml";
    private static final byte[] DEFAULT_MERGE_CONFIG_FILE =
            IoUtils.readInputStreamToBytes(MergeValidateServiceTest.class.getClassLoader().getResourceAsStream("validation/default.txt"));

    private final Github github = mock(Github.class);

    private final MergeValidate service =
            new MergeValidateService(github, MERGE_FILE_NAME, new Charset(StandardCharsets.UTF_8));

    public static Iterable<Object[]> validResponses() {
        return Arrays.asList(
                new Object[]{"validation/merge_file_allowed.txt", new CommitDifference(1, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, "README.md")))},
                new Object[]{"validation/workflow_dir_allowed.txt", new CommitDifference(1, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, "README_TEMPLATE.md")))},
                new Object[]{"validation/all_but_exclude_one.txt", new CommitDifference(1, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, "some/where/inside/file.txt")))}
        );
    }

    @ParameterizedTest
    @MethodSource("validResponses")
    public void verify_github_merge_success(String configFile, CommitDifference diff) {
        when(github.getFileContent(eq(AUTHORIZATION), eq(USER), eq(REPO), eq(DEST_BRANCH), eq(MERGE_FILE_NAME)))
                .thenReturn(IoUtils.readInputStreamToBytes(getClass().getClassLoader().getResourceAsStream(configFile)));
        when(github.listChanges(eq(AUTHORIZATION), eq(USER), eq(REPO), eq(DEST_BRANCH), eq(SOURCE_BRANCH)))
                .thenReturn(diff);
        service.merge(AUTHORIZATION, USER, REPO, SOURCE_BRANCH, DEST_BRANCH);
    }

    public static final List<FileChange> CHANGES = Arrays.asList(new FileChange(FileChange.Type.CHANGED, ".github/workflows/release.yml"),
            new FileChange(FileChange.Type.ADDED, ".merge-validate"),
            new FileChange(FileChange.Type.CHANGED, "README.md"));


    public static Iterable<Object[]> invalidDiffResponses() {
        return Arrays.asList(
                new Object[]{"not fast forward", new CommitDifference(0, 5, Collections.emptyList())},
                new Object[]{"not fast forward", new CommitDifference(5, 1, CHANGES)},
                new Object[]{"illegal changes", new CommitDifference(5, 0, CHANGES)},
                new Object[]{"illegal changes", new CommitDifference(5, 0, Arrays.asList(CHANGES.get(0), CHANGES.get(2)))},
                new Object[]{"illegal changes", new CommitDifference(5, 0, Arrays.asList(CHANGES.get(1), CHANGES.get(2)))},
                new Object[]{"illegal changes", new CommitDifference(5, 0, Collections.singletonList(CHANGES.get(0)))},
                new Object[]{"illegal changes", new CommitDifference(5, 0, Collections.singletonList(CHANGES.get(1)))},
                new Object[]{"illegal changes", new CommitDifference(5, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, "src/main/resources/reportportal.properties")))},
                new Object[]{"illegal changes", new CommitDifference(5, 0, Collections.singletonList(new FileChange(FileChange.Type.ADDED, "README.md")))}
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDiffResponses")
    public void verify_invalid_diff_responses(String expectedMessage, CommitDifference response) {
        when(github.getFileContent(eq(AUTHORIZATION), eq(USER), eq(REPO), eq(DEST_BRANCH), eq(MERGE_FILE_NAME)))
                .thenReturn(DEFAULT_MERGE_CONFIG_FILE);
        when(github.listChanges(eq(AUTHORIZATION), eq(USER), eq(REPO), eq(DEST_BRANCH), eq(SOURCE_BRANCH)))
                .thenReturn(response);

        IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.merge(AUTHORIZATION, USER, REPO, SOURCE_BRANCH, DEST_BRANCH));
        assertThat(result.getMessage(), Matchers.endsWith(expectedMessage));
    }

    public static Iterable<Object[]> invalidAllowanceResponses() {
        return Arrays.asList(
                new Object[]{"validation/merge_file_allowed.txt", new CommitDifference(1, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, MERGE_FILE_NAME)))},
                new Object[]{"validation/workflow_dir_allowed.txt", new CommitDifference(1, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, WORKFLOW_FILE)))},
                new Object[]{"validation/workflow_file_allowed.txt", new CommitDifference(1, 0, Collections.singletonList(new FileChange(FileChange.Type.CHANGED, WORKFLOW_FILE)))}
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAllowanceResponses")
    public void verify_allowance_exceptions(String configFilePath, CommitDifference difference) {
        byte[] configFile =
                IoUtils.readInputStreamToBytes(getClass().getClassLoader().getResourceAsStream(configFilePath));

        when(github.getFileContent(eq(AUTHORIZATION), eq(USER), eq(REPO), eq(DEST_BRANCH), eq(MERGE_FILE_NAME)))
                .thenReturn(configFile);
        when(github.listChanges(eq(AUTHORIZATION), eq(USER), eq(REPO), eq(DEST_BRANCH), eq(SOURCE_BRANCH)))
                .thenReturn(difference);

        IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.merge(AUTHORIZATION, USER, REPO, SOURCE_BRANCH, DEST_BRANCH));
        assertThat(result.getMessage(), Matchers.endsWith("illegal changes"));
    }
}
