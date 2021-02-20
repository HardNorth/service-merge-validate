package net.hardnorth.github.merge.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ValidationPatternTest {


    public static List<Object[]> patternValues() {
        return Arrays.asList(new Object[]{"validation/simple.txt", "mvnw.cmd", Boolean.TRUE},
                new Object[]{"validation/wildcard.txt", "README.md", Boolean.TRUE},
                new Object[]{"validation/root_dir.txt", "mvnw.cmd", Boolean.TRUE},
                new Object[]{"validation/all_txt.txt", "validation/all_txt.txt", Boolean.TRUE},
                new Object[]{"validation/all_but_exclude_one.txt", "validation/all_but_exclude_one.txt", Boolean.FALSE});
    }

    @ParameterizedTest
    @MethodSource("patternValues")
    public void pattern_tests(String patternFile, String testFile, Boolean result) throws FileNotFoundException {
        String file = IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream(patternFile));
        ValidationPattern pattern = ValidationPattern.parse(ofNullable(file).orElseThrow(FileNotFoundException::new));

        assertThat(pattern.test(new File(testFile).toPath()), equalTo(result));
    }

}
