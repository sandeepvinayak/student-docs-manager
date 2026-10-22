package com.classroom.docmanager.config;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.CollectionOptions;

/**
 * Factory for substrate-neutral storage clients.
 *
 * The substrate is selected at runtime via environment variables, and the matching provider
 * implementation must be on the classpath (selected at build time via Maven profile):
 *   BLOB_PROVIDER     - blob provider id (default "aws"; e.g. "gcp", "ali")
 *   DOCSTORE_PROVIDER - docstore provider id (default "aws"; e.g. "gcp-firestore", "ali")
 *   SUBSTRATE_REGION  - region (default "us-west-2")
 *
 * Credentials come from the provider's default credential chain.
 */
public class SubstrateConfig {

    public static final String DOCUMENTS_TABLE = "StudentDocuments";
    public static final String PARTITION_KEY = "studentId";
    public static final String SORT_KEY = "documentId";

    private static final String BLOB_PROVIDER = env("BLOB_PROVIDER", "aws");
    private static final String DOCSTORE_PROVIDER = env("DOCSTORE_PROVIDER", "aws");
    private static final String REGION = env("SUBSTRATE_REGION", "us-west-2");

    public static BucketClient bucketClient(String bucketName) {
        return BucketClient.builder(BLOB_PROVIDER)
                .withRegion(REGION)
                .withBucket(bucketName)
                .build();
    }

    public static DocStoreClient docStoreClient() {
        CollectionOptions collectionOptions = new CollectionOptions.CollectionOptionsBuilder()
                .withTableName(DOCUMENTS_TABLE)
                .withPartitionKey(PARTITION_KEY)
                .withSortKey(SORT_KEY)
                .build();

        return DocStoreClient.builder(DOCSTORE_PROVIDER)
                .withRegion(REGION)
                .withCollectionOptions(collectionOptions)
                .build();
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
