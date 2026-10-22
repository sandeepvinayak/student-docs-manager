package com.classroom.docmanager.config;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.CollectionOptions;

/**
 * Factory for Substrate SDK (MultiCloudJ) clients.
 *
 * The cloud provider and region are configurable so the application code stays substrate-neutral:
 *   - CLOUD_PROVIDER (env) / cloud.provider (system property), default "aws"
 *   - CLOUD_REGION   (env) / cloud.region   (system property), default "us-west-2"
 *
 * Credentials are resolved by the provider's default credential chain
 * (env vars, ~/.aws/credentials, IAM role, etc. for AWS).
 */
public class SubstrateConfig {

    public static final String TABLE_NAME = "StudentDocuments";
    public static final String PARTITION_KEY = "studentId";
    public static final String SORT_KEY = "documentId";

    private static final String PROVIDER = setting("cloud.provider", "CLOUD_PROVIDER", "aws");
    private static final String REGION = setting("cloud.region", "CLOUD_REGION", "us-west-2");

    private SubstrateConfig() {}

    public static BucketClient bucketClient(String bucketName) {
        return BucketClient.builder(PROVIDER)
                .withBucket(bucketName)
                .withRegion(REGION)
                .build();
    }

    public static DocStoreClient docStoreClient() {
        CollectionOptions options = new CollectionOptions.CollectionOptionsBuilder()
                .withTableName(TABLE_NAME)
                .withPartitionKey(PARTITION_KEY)
                .withSortKey(SORT_KEY)
                .build();

        return DocStoreClient.builder(PROVIDER)
                .withRegion(REGION)
                .withCollectionOptions(options)
                .build();
    }

    private static String setting(String sysProp, String envVar, String defaultValue) {
        String value = System.getProperty(sysProp);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
