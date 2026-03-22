package com.classroom.docmanager.config;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.CollectionOptions;

/**
 * Factory for Substrate SDK service clients.
 * Uses default credential chain for the configured provider.
 */
public class SubstrateConfig {

    private static final String REGION = "us-west-2";

    public static BucketClient bucketClient(String provider, String bucketName) {
        return BucketClient.builder(provider)
                .withBucket(bucketName)
                .withRegion(REGION)
                .build();
    }

    public static DocStoreClient docStoreClient(String provider, String tableName) {
        CollectionOptions collectionOptions = new CollectionOptions.CollectionOptionsBuilder()
                .withTableName(tableName)
                .withPartitionKey("studentId")
                .withSortKey("documentId")
                .build();

        return DocStoreClient.builder(provider)
                .withRegion(REGION)
                .withCollectionOptions(collectionOptions)
                .build();
    }
}
