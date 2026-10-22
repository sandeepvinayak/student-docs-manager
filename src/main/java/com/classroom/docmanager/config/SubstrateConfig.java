package com.classroom.docmanager.config;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.CollectionOptions;

/**
 * Factory for Substrate SDK (MultiCloudJ) clients.
 * The cloud provider is externalized (env var SUBSTRATE_PROVIDER, default "aws");
 * credentials come from the provider's default credential chain.
 */
public class SubstrateConfig {

    public static final String TABLE_NAME = "StudentDocuments";
    public static final String PARTITION_KEY = "studentId";
    public static final String SORT_KEY = "documentId";

    private static final String PROVIDER = env("SUBSTRATE_PROVIDER", "aws");
    private static final String REGION = env("SUBSTRATE_REGION", "us-west-2");

    public static BucketClient bucketClient(String bucketName) {
        return BucketClient.builder(PROVIDER)
                .withBucket(bucketName)
                .withRegion(REGION)
                .build();
    }

    public static DocStoreClient docStoreClient() {
        return DocStoreClient.builder(PROVIDER)
                .withRegion(REGION)
                .withCollectionOptions(new CollectionOptions.CollectionOptionsBuilder()
                        .withTableName(TABLE_NAME)
                        .withPartitionKey(PARTITION_KEY)
                        .withSortKey(SORT_KEY)
                        .build())
                .build();
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
