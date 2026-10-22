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
 * Handles all interactions with the blob store via the Substrate SDK {@link BucketClient}.
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
        long size;
        try {
            size = Files.size(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read size of " + filePath, e);
        }

        UploadRequest request = UploadRequest.builder()
                .withKey(key)
                .withContentType(contentType)
                .withContentLength(size)
                .build();

        bucket.upload(request, filePath);
        log.info("Uploaded {} -> {}/{}", filePath.getFileName(), bucket.getBucket(), key);
    }

    /** Download a blob and return its bytes. */
    public byte[] downloadFile(String key) {
        ByteArray content = new ByteArray();
        bucket.download(DownloadRequest.builder().withKey(key).build(), content);
        byte[] bytes = content.getBytes();
        log.info("Downloaded {}/{} ({} bytes)", bucket.getBucket(), key, bytes.length);
        return bytes;
    }

    /** List all blob keys under a given prefix (e.g., "students/stu001/"). Paginates automatically. */
    public List<String> listFiles(String prefix) {
        Iterator<BlobInfo> it = bucket.list(ListBlobsRequest.builder().withPrefix(prefix).build());
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next().getKey());
        }
        return keys;
    }

    /** Delete a single blob. */
    public void deleteFile(String key) {
        bucket.delete(key, null);
        log.info("Deleted {}/{}", bucket.getBucket(), key);
    }
}
