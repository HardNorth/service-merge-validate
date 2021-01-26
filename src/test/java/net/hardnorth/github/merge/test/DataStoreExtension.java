package net.hardnorth.github.merge.test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.logging.Logger;

public class DataStoreExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Logger LOGGER = Logger.getLogger(DataStoreExtension.class.getSimpleName());

    private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

    public static Datastore getDataStore(String projectId) {
        return HELPER.getOptions().toBuilder().setProjectId(projectId).build().getService();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        LOGGER.info("Starting Datastore mock");
        try {
            HELPER.start();
        } catch (Exception e) {
            LOGGER.throwing(LocalDatastoreHelper.class.getName(), "start", e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        try {
            HELPER.stop();
        } catch (Exception e) {
            LOGGER.throwing(LocalDatastoreHelper.class.getName(), "stop", e);
        }

    }
}
