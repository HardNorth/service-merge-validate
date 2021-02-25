package net.hardnorth.github.merge.service;

import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.service.impl.MergeValidateService;
import net.hardnorth.github.merge.utils.IoUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MergeValidateServiceTest {
    private static final String MERGE_FILE_NAME = ".merge-validate";
    private static final String AUTHORIZATION = "auth";
    private static final String REPO = "HardNorth/test";
    private static final String SOURCE_BRANCH = "source";
    private static final String DEST_BRANCH = "dest";
    private final Github github = mock(Github.class);


    private final MergeValidate service =
            new MergeValidateService(github, MERGE_FILE_NAME, new Charset(StandardCharsets.UTF_8));

    @Test
    public void verify_github_merge_success() {
        when(github.getFileContent(eq(AUTHORIZATION), eq(REPO), eq(DEST_BRANCH), eq(MERGE_FILE_NAME)))
                .thenReturn(IoUtils.readInputStreamToBytes(getClass().getClassLoader().getResourceAsStream("validation/default.txt")));
        service.merge(AUTHORIZATION, REPO, SOURCE_BRANCH, DEST_BRANCH);
    }

}
