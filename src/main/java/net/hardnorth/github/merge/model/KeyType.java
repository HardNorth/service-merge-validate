package net.hardnorth.github.merge.model;

public enum KeyType {
    LONG, STRING;

    public static KeyType getKeyType(Object key) {
        if (key instanceof Number) {
            return LONG;
        }
        if (key instanceof String) {
            return STRING;
        }
        throw new IllegalArgumentException("Unknown key type");
    }
}
