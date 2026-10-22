package com.classroom.docmanager.service;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.blob.driver.BlobInfo;
import com.salesforce.multicloudj.blob.driver.ByteArray;
import com.salesforce.multicloudj.blob.driver.DownloadRequest;
import com.salesforce.multicloudj.blob.driver.ListBlobsRequest;
import com.salesforce.multicloudj.blob.driver.UploadRequest;
import com.salesforce.multicloudj.common.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlobStoreServiceTest {

    private BucketClient bucketClient;
    private BlobStoreService service;

    @BeforeEach
    void setUp() {
        bucketClient = mock(BucketClient.class);
        service = new BlobStoreService(bucketClient, "test-bucket");
    }

    @Test
    void uploadFile_shouldUploadWithKeyLengthAndContentType(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("essay.txt");
        Files.writeString(file, "hello");

        service.uploadFile("students/stu001/essay.txt", file, "text/plain");

        ArgumentCaptor<UploadRequest> captor = ArgumentCaptor.forClass(UploadRequest.class);
        verify(bucketClient).upload(captor.capture(), eq(file));
        UploadRequest req = captor.getValue();
        assertEquals("students/stu001/essay.txt", req.getKey());
        assertEquals(5L, req.getContentLength());
        assertEquals("text/plain", req.getContentType());
    }

    @Test
    void downloadFile_shouldReturnBytes() {
        doAnswer(inv -> {
            ByteArray out = inv.getArgument(1);
            out.setBytes("content".getBytes());
            return null;
        }).when(bucketClient).download(any(DownloadRequest.class), any(ByteArray.class));

        byte[] result = service.downloadFile("students/stu001/essay.txt");

        assertArrayEquals("content".getBytes(), result);
        ArgumentCaptor<DownloadRequest> captor = ArgumentCaptor.forClass(DownloadRequest.class);
        verify(bucketClient).download(captor.capture(), any(ByteArray.class));
        assertEquals("students/stu001/essay.txt", captor.getValue().getKey());
    }

    @Test
    void downloadFile_shouldPropagateNotFound() {
        doThrow(new ResourceNotFoundException("missing"))
                .when(bucketClient).download(any(DownloadRequest.class), any(ByteArray.class));

        assertThrows(ResourceNotFoundException.class, () -> service.downloadFile("nope"));
    }

    @Test
    void listFiles_shouldReturnKeysUnderPrefix() {
        when(bucketClient.list(any(ListBlobsRequest.class))).thenReturn(List.of(
                BlobInfo.builder().withKey("students/stu001/a.txt").build(),
                BlobInfo.builder().withKey("students/stu001/b.txt").build()).iterator());

        List<String> keys = service.listFiles("students/stu001/");

        assertEquals(List.of("students/stu001/a.txt", "students/stu001/b.txt"), keys);
        ArgumentCaptor<ListBlobsRequest> captor = ArgumentCaptor.forClass(ListBlobsRequest.class);
        verify(bucketClient).list(captor.capture());
        assertEquals("students/stu001/", captor.getValue().getPrefix());
    }

    @Test
    void deleteFile_shouldDeleteLatestVersion() {
        service.deleteFile("students/stu001/a.txt");

        verify(bucketClient).delete("students/stu001/a.txt", null);
    }
}
