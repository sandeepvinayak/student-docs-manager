package com.classroom.docmanager.service;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.blob.driver.BlobInfo;
import com.salesforce.multicloudj.blob.driver.ByteArray;
import com.salesforce.multicloudj.blob.driver.DownloadRequest;
import com.salesforce.multicloudj.blob.driver.ListBlobsRequest;
import com.salesforce.multicloudj.blob.driver.UploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles all interactions with blob storage via the Substrate SDK BucketClient.
 * Supports upload, download, list, and delete operations on student documents.
 */
public class BlobStoreService {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreService.class);

    private final BucketClient bucket;

    public BlobStoreService(BucketClient bucket) {
        this.bucket = bucket;
    }

    /** Upload a file to the bucket under the given key. */
    public void uploadFile(String key, Path filePath, String contentType) {
        UploadRequest request = UploadRequest.builder()
                .withKey(key)
                .withContentType(contentType)
                .build();

        bucket.upload(request, filePath);
        log.info("Uploaded {} -> {}/{}", filePath.getFileName(), bucket.getBucket(), key);
    }

    /** Download a file from the bucket and return its bytes. */
    public byte[] downloadFile(String key) {
        DownloadRequest request = DownloadRequest.builder()
                .withKey(key)
                .build();

        ByteArray content = new ByteArray();
        bucket.download(request, content);
        log.info("Downloaded {}/{} ({} bytes)", bucket.getBucket(), key, content.getBytes().length);
        return content.getBytes();
    }

    /** List all object keys under a given prefix (e.g., "students/stu001/"). */
    public List<String> listFiles(String prefix) {
        ListBlobsRequest request = ListBlobsRequest.builder()
                .withPrefix(prefix)
                .build();

        List<String> keys = new ArrayList<>();
        Iterator<BlobInfo> blobs = bucket.list(request);
        blobs.forEachRemaining(blob -> keys.add(blob.getKey()));
        return keys;
    }

    /** Delete a single object from the bucket. */
    public void deleteFile(String key) {
        bucket.delete(key, null);
        log.info("Deleted {}/{}", bucket.getBucket(), key);
    }
}
