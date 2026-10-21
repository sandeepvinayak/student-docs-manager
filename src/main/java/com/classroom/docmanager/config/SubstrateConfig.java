package com.classroom.docmanager.config;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.CollectionOptions;

/**
 * Factory for Substrate SDK (multicloudj) clients.
 * The substrate is selected at runtime via SUBSTRATE_PROVIDER / SUBSTRATE_REGION; the matching
 * provider implementation is pulled in at build time by the Maven profile (-Paws, -Pgcp, ...).
 * Credentials come from the provider's default credential chain.
 */
public class SubstrateConfig {

    private static final String PROVIDER = env("SUBSTRATE_PROVIDER", "aws");
    private static final String REGION = env("SUBSTRATE_REGION", "us-west-2");

    public static BucketClient bucketClient(String bucketName) {
        return BucketClient.builder(PROVIDER)
                .withRegion(REGION)
                .withBucket(bucketName)
                .build();
    }

    public static DocStoreClient docStoreClient(String tableName, String partitionKey, String sortKey) {
        CollectionOptions options = new CollectionOptions.CollectionOptionsBuilder()
                .withTableName(tableName)
                .withPartitionKey(partitionKey)
                .withSortKey(sortKey)
                .build();

        return DocStoreClient.builder(PROVIDER)
                .withRegion(REGION)
                .withCollectionOptions(options)
                .build();
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
