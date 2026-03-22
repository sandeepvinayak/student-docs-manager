package com.classroom.docmanager.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Factory for AWS service clients.
 * Uses default credential chain (env vars, ~/.aws/credentials, IAM role, etc.)
 */
public class AwsConfig {

    private static final Region REGION = Region.US_WEST_2;

    public static S3Client s3Client() {
        return S3Client.builder()
                .region(REGION)
                .build();
    }

    public static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(REGION)
                .build();
    }
}
