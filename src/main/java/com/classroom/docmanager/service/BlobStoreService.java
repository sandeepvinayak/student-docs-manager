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
import java.util.Map;

/**
 * Handles all interactions with Blob storage (Substrate SDK).
 * Supports upload, download, list, and delete operations on student documents.
 */
public class BlobStoreService {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreService.class);

    private final BucketClient bucketClient;
    private final String bucketName;

    public BlobStoreService(BucketClient bucketClient, String bucketName) {
        this.bucketClient = bucketClient;
        this.bucketName = bucketName;
    }

    /** Upload a file under the given key. */
    public void uploadFile(String key, Path filePath, String contentType) {
        UploadRequest request = new UploadRequest.Builder()
                .withKey(key)
                .withMetadata(Map.of("Content-Type", contentType))
                .build();

        bucketClient.upload(request, filePath);
        log.info("Uploaded {} -> {}/{}", filePath.getFileName(), bucketName, key);
    }

    /** Download a file and return its bytes. */
    public byte[] downloadFile(String key) {
        DownloadRequest request = new DownloadRequest.Builder()
                .withKey(key)
                .build();

        ByteArray byteArray = new ByteArray();
        bucketClient.download(request, byteArray);
        byte[] data = byteArray.getBytes();
        log.info("Downloaded {}/{} ({} bytes)", bucketName, key, data.length);
        return data;
    }

    /** List all object keys under a given prefix. */
    public List<String> listFiles(String prefix) {
        ListBlobsRequest request = new ListBlobsRequest.Builder()
                .withPrefix(prefix)
                .build();

        Iterator<BlobInfo> iterator = bucketClient.list(request);
        List<String> keys = new ArrayList<>();
        while (iterator.hasNext()) {
            keys.add(iterator.next().getKey());
        }
        return keys;
    }

    /** Delete a single object. */
    public void deleteFile(String key) {
        bucketClient.delete(key, null);
        log.info("Deleted {}/{}", bucketName, key);
    }
}
