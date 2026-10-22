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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BlobStoreServiceTest {

    private BucketClient bucketClient;
    private BlobStoreService service;

    @BeforeEach
    void setUp() {
        bucketClient = mock(BucketClient.class);
        when(bucketClient.getBucket()).thenReturn("classroom-student-docs");
        service = new BlobStoreService(bucketClient);
    }

    @Test
    void uploadFile_shouldUploadWithKeyContentTypeAndLength(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("homework.txt");
        Files.writeString(file, "This is my homework.");

        service.uploadFile("students/stu001/doc_homework.txt", file, "text/plain");

        ArgumentCaptor<UploadRequest> captor = ArgumentCaptor.forClass(UploadRequest.class);
        verify(bucketClient).upload(captor.capture(), eq(file));
        UploadRequest req = captor.getValue();
        assertEquals("students/stu001/doc_homework.txt", req.getKey());
        assertEquals("text/plain", req.getContentType());
        assertEquals(20L, req.getContentLength());
    }

    @Test
    void downloadFile_shouldReturnBytes() {
        doAnswer(inv -> {
            ByteArray out = inv.getArgument(1);
            out.setBytes("file content".getBytes());
            return null;
        }).when(bucketClient).download(any(DownloadRequest.class), any(ByteArray.class));

        byte[] result = service.downloadFile("students/stu001/doc_homework.txt");

        assertArrayEquals("file content".getBytes(), result);
        ArgumentCaptor<DownloadRequest> captor = ArgumentCaptor.forClass(DownloadRequest.class);
        verify(bucketClient).download(captor.capture(), any(ByteArray.class));
        assertEquals("students/stu001/doc_homework.txt", captor.getValue().getKey());
    }

    @Test
    void downloadFile_shouldPropagateNotFound() {
        doThrow(new ResourceNotFoundException("missing"))
                .when(bucketClient).download(any(DownloadRequest.class), any(ByteArray.class));

        assertThrows(ResourceNotFoundException.class, () -> service.downloadFile("missing"));
    }

    @Test
    void listFiles_shouldReturnAllKeysUnderPrefix() {
        BlobInfo a = BlobInfo.builder().withKey("students/stu001/a.txt").build();
        BlobInfo b = BlobInfo.builder().withKey("students/stu001/b.txt").build();
        when(bucketClient.list(any(ListBlobsRequest.class))).thenReturn(List.of(a, b).iterator());

        List<String> keys = service.listFiles("students/stu001/");

        assertEquals(List.of("students/stu001/a.txt", "students/stu001/b.txt"), keys);
        ArgumentCaptor<ListBlobsRequest> captor = ArgumentCaptor.forClass(ListBlobsRequest.class);
        verify(bucketClient).list(captor.capture());
        assertEquals("students/stu001/", captor.getValue().getPrefix());
    }

    @Test
    void deleteFile_shouldDeleteKey() {
        service.deleteFile("students/stu001/doc_homework.txt");

        verify(bucketClient).delete("students/stu001/doc_homework.txt", null);
    }
}
