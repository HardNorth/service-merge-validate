package net.hardnorth.github.merge.config;

public class PropertyNames {

    private PropertyNames() {
    }

    public static final String APPLICATION_NAME = "net.hardnorth.application.name";
    public static final String APPLICATION_URL = "net.hardnorth.application.url";
    public static final String PROJECT_ID = "net.hardnorth.application.project";
    public static final String CHARSET = "net.hardnorth.application.charset";
    public static final String ENCRYPTION_KEY_SECRET = "net.hardnorth.encryption.key.secret";

    // Github
    public static final String GITHUB_LOG = "net.hardnorth.github.http.log";
    public static final String GITHUB_API_URL = "net.hardnorth.github.api.url";
    public static final String GITHUB_APP_ID = "net.hardnorth.github.app.id";
    public static final String GITHUB_RSA_KEY_SECRET = "net.hardnorth.github.rsa.key.secret";
    public static final String GITHUB_WEBHOOK_TOKEN_SECRET = "net.hardnorth.github.webhook.token.secret";
    public static final String GITHUB_TIMEOUT_UNIT = "net.hardnorth.github.timeout.unit";
    public static final String GITHUB_TIMEOUT_VALUE = "net.hardnorth.github.timeout.value";
    public static final String GITHUB_FILE_SIZE_LIMIT = "net.hardnorth.github.file.size.limit";
}
