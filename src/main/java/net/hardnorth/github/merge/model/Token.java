package net.hardnorth.github.merge.model;

import net.hardnorth.github.merge.utils.Keys;

import java.util.UUID;

public class Token {
    private final byte[] value;

    public Token(UUID source) {
        value = Keys.getBytes(source);
    }

    public Token(byte[] token) {
        value = token;
    }

    public byte[] getValue() {
        return value;
    }
}
