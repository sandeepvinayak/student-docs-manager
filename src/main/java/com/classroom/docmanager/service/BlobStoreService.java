package com.classroom.docmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Handles all interactions with S3 (blobstore).
 * Supports upload, download, list, and delete operations on student documents.
 */
public class BlobStoreService {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreService.class);

    private final S3Client s3;
    private final String bucketName;

    public BlobStoreService(S3Client s3, String bucketName) {
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    /** Upload a file to S3 under the given key. */
    public void uploadFile(String s3Key, Path filePath, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        s3.putObject(request, RequestBody.fromFile(filePath));
        log.info("Uploaded {} -> s3://{}/{}", filePath.getFileName(), bucketName, s3Key);
    }

    /** Download a file from S3 and return its bytes. */
    public byte[] downloadFile(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(request);
        log.info("Downloaded s3://{}/{} ({} bytes)", bucketName, s3Key, response.asByteArray().length);
        return response.asByteArray();
    }

    /** List all object keys under a given prefix (e.g., "students/stu001/"). */
    public List<String> listFiles(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .toList();
    }

    /** Delete a single object from S3. */
    public void deleteFile(String s3Key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
        log.info("Deleted s3://{}/{}", bucketName, s3Key);
    }
}
