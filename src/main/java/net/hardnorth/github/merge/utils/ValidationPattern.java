package net.hardnorth.github.merge.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("CdiInjectionPointsInspection")
public class ValidationPattern implements Predicate<Path>{

    private static class Rule implements Predicate<Path> {

        public enum RuleType {
            INCLUDE, EXCLUDE
        }

        private final RuleType type;
        private final PathMatcher matcher;

        public Rule(RuleType ruleType, String rulePattern) {
            type = ruleType;
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + rulePattern);
        }

        public boolean test(Path filePath) {
            return matcher.matches(filePath);
        }
    }

    private final List<Rule> rules;

    private ValidationPattern(List<Rule> ruleList) {
        rules = ruleList;
    }

    public boolean test(Path path) {
        List<Rule> matchedRules = rules.stream().filter(r -> r.test(path)).collect(Collectors.toList());
        boolean result = false;
        for (Rule r: matchedRules) {
            if(Rule.RuleType.INCLUDE == r.type) {
                result = true;
            } else {
                result = false;
            }
        }
        return result;
    }

    public static ValidationPattern parse(String pattern) {
        String[] content = pattern.split("\\r?\\n");
        List<Rule> result = Arrays.stream(content).filter(l -> !l.isBlank()).filter(l -> !l.startsWith("#")).map(l -> {
            if (l.startsWith("!")) {
                return new Rule(Rule.RuleType.EXCLUDE, l.substring(1));
            } else if (l.startsWith("\\") && l.length() > 1 && ('#' == l.charAt(1) || '!' == l.charAt(1) || '\\' == l.charAt(1))) {
                return new Rule(Rule.RuleType.INCLUDE, l.substring(1));
            } else {
                return new Rule(Rule.RuleType.INCLUDE, l);
            }
        }).collect(Collectors.toList());
        return new ValidationPattern(result);
    }

}
