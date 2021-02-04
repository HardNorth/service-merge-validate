package net.hardnorth.github.merge.service;

public interface EncryptedStorage {
    String getValue(String key);

    void saveValue(String key, String value);
}
