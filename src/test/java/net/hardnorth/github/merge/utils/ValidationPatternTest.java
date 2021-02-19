package net.hardnorth.github.merge.utils;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ValidationPatternTest {

    @Test
    public void pattern_tests() {
        String file = IoUtils.readInputStreamToString(getClass().getClassLoader().getResourceAsStream("validation/simple.txt"));
        ValidationPattern pattern = ValidationPattern.parse(file);

        assertThat(pattern.test(new File("mvnw.cmd").toPath()), equalTo(Boolean.TRUE));
    }

}
