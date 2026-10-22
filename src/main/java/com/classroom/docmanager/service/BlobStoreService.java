package com.classroom.docmanager.service;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.blob.driver.BlobInfo;
import com.salesforce.multicloudj.blob.driver.ByteArray;
import com.salesforce.multicloudj.blob.driver.DownloadRequest;
import com.salesforce.multicloudj.blob.driver.ListBlobsRequest;
import com.salesforce.multicloudj.blob.driver.UploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles all interactions with the blob store (Substrate SDK BucketClient).
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

    /** Upload a file to the bucket under the given key. */
    public void uploadFile(String key, Path filePath, String contentType) {
        long size;
        try {
            size = Files.size(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read file " + filePath, e);
        }

        UploadRequest request = UploadRequest.builder()
                .withKey(key)
                .withContentLength(size)
                .withContentType(contentType)
                .build();

        bucketClient.upload(request, filePath);
        log.info("Uploaded {} -> {}/{}", filePath.getFileName(), bucketName, key);
    }

    /** Download a file from the bucket and return its bytes. */
    public byte[] downloadFile(String key) {
        DownloadRequest request = DownloadRequest.builder()
                .withKey(key)
                .build();

        ByteArray content = new ByteArray();
        bucketClient.download(request, content);
        byte[] bytes = content.getBytes();
        log.info("Downloaded {}/{} ({} bytes)", bucketName, key, bytes.length);
        return bytes;
    }

    /** List all object keys under a given prefix (e.g., "students/stu001/"). */
    public List<String> listFiles(String prefix) {
        ListBlobsRequest request = ListBlobsRequest.builder()
                .withPrefix(prefix)
                .build();

        List<String> keys = new ArrayList<>();
        Iterator<BlobInfo> it = bucketClient.list(request);
        while (it.hasNext()) {
            keys.add(it.next().getKey());
        }
        return keys;
    }

    /** Delete a single object from the bucket. */
    public void deleteFile(String key) {
        bucketClient.delete(key, null);
        log.info("Deleted {}/{}", bucketName, key);
    }
}
