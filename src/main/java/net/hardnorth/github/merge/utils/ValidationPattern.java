package net.hardnorth.github.merge.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("CdiInjectionPointsInspection")
public class ValidationPattern implements Predicate<Path> {

    private static class Rule implements Predicate<Path> {

        public enum RuleType {
            INCLUDE, EXCLUDE
        }

        private final RuleType type;
        private final PathMatcher matcher;

        private static String normalizePattern(String pattern) {
            return pattern.startsWith("./") ? pattern.substring(2) : pattern;
        }

        public Rule(RuleType ruleType, String rulePattern) {
            type = ruleType;
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + normalizePattern(rulePattern));
        }

        public boolean test(Path filePath) {
            return matcher.matches(filePath);
        }
    }

    private final List<Rule> rules;

    ValidationPattern(List<Rule> ruleList) {
        rules = ruleList;
    }

    public boolean test(Path path) {
        List<Rule> matchedRules = rules.stream().filter(r -> r.test(path)).collect(Collectors.toList());
        boolean result = false;
        for (Rule r : matchedRules) {
            result = Rule.RuleType.INCLUDE == r.type;
        }
        return result;
    }

    private static Rule toRule(String ruleStr) {
        if (ruleStr.startsWith("!")) {
            return new Rule(Rule.RuleType.EXCLUDE, ruleStr.substring(1));
        } else if (ruleStr.startsWith("\\") && ruleStr.length() > 1 && ('#' == ruleStr.charAt(1) || '!' == ruleStr.charAt(1) || '\\' == ruleStr.charAt(1))) {
            return new Rule(Rule.RuleType.INCLUDE, ruleStr.substring(1));
        } else {
            return new Rule(Rule.RuleType.INCLUDE, ruleStr);
        }
    }

    public void addRule(String ruleStr) {
        rules.add(toRule(ruleStr));
    }

    public static ValidationPattern parse(String pattern) {
        String[] content = pattern.split("\\r?\\n");
        List<Rule> result = Arrays.stream(content)
                .filter(l -> !l.isBlank())
                .filter(l -> !l.startsWith("#"))
                .map(ValidationPattern::toRule)
                .collect(Collectors.toList());
        return new ValidationPattern(result);
    }
}
