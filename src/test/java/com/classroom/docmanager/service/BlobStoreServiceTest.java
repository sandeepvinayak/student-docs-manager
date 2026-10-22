package com.classroom.docmanager.service;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.blob.inmemory.InMemoryBlobStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises BlobStoreService against the Substrate SDK's in-memory blob provider. */
class BlobStoreServiceTest {

    private BlobStoreService blobStore;

    @BeforeEach
    void setUp() {
        // In-memory storage is static (shared across clients), so isolate each test in its own bucket
        String bucketName = "test-bucket-" + UUID.randomUUID();
        InMemoryBlobStore.createBucket(bucketName);

        BucketClient bucket = BucketClient.builder("memory")
                .withRegion("local")
                .withBucket(bucketName)
                .build();
        blobStore = new BlobStoreService(bucket);
    }

    @Test
    void uploadThenDownload_shouldRoundTripContent(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("homework.txt");
        Files.writeString(file, "This is my homework.");

        blobStore.uploadFile("students/stu001/doc1_homework.txt", file, "text/plain");

        assertArrayEquals("This is my homework.".getBytes(),
                blobStore.downloadFile("students/stu001/doc1_homework.txt"));
    }

    @Test
    void listFiles_shouldReturnOnlyKeysUnderPrefix(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("f.txt");
        Files.writeString(file, "x");

        blobStore.uploadFile("students/stu001/a.txt", file, "text/plain");
        blobStore.uploadFile("students/stu001/b.txt", file, "text/plain");
        blobStore.uploadFile("students/stu002/c.txt", file, "text/plain");

        List<String> keys = blobStore.listFiles("students/stu001/");

        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(List.of("students/stu001/a.txt", "students/stu001/b.txt")));
    }

    @Test
    void deleteFile_shouldRemoveObject(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("f.txt");
        Files.writeString(file, "x");
        blobStore.uploadFile("students/stu001/a.txt", file, "text/plain");

        blobStore.deleteFile("students/stu001/a.txt");

        assertTrue(blobStore.listFiles("students/stu001/").isEmpty());
    }
}
