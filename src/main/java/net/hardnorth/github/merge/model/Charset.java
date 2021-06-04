package net.hardnorth.github.merge.model;

public class Charset {
    private final java.nio.charset.Charset value;

    public Charset(java.nio.charset.Charset charset) {
        value = charset;
    }

    public java.nio.charset.Charset get() {
        return value;
    }
}
