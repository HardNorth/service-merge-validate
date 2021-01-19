package net.hardnorth.github.merge.utils;

public class StringUtils {
    public static final String DEFAULT_FORMAT_PLACEHOLDER = "{}";

    public static String simpleFormat(String pattern, Object... replacements) {
        String placeholder = DEFAULT_FORMAT_PLACEHOLDER;
        int phLength = placeholder.length();
        if (pattern.length() < phLength) {
            return pattern;
        }

        int pos = pattern.indexOf(placeholder);
        if (pos < 0) {
            return pattern;
        }

        if (replacements == null) {
            replacements = new Object[0];
        }

        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        int i = 0;
        do {
            result.append(pattern, lastPos, pos);
            if (i < replacements.length) {
                result.append(replacements[i++]);
            }
            lastPos = pos + phLength;
            pos = pattern.indexOf(placeholder, lastPos);
        } while (pos > 0);
        result.append(pattern, lastPos, pattern.length());

        return result.toString();
    }
}
