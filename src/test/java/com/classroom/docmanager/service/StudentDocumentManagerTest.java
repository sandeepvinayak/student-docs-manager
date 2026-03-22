package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
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

class StudentDocumentManagerTest {

    private BlobStoreService blobStore;
    private DocumentMetadataService metadataService;
    private StudentDocumentManager manager;

    @BeforeEach
    void setUp() {
        blobStore = mock(BlobStoreService.class);
        metadataService = mock(DocumentMetadataService.class);
        manager = new StudentDocumentManager(blobStore, metadataService);
    }

    @Test
    void uploadAssignment_shouldUploadToBlobStoreAndSaveMetadata(@TempDir Path tempDir) throws IOException {
        // Create a temp file to upload
        Path testFile = tempDir.resolve("homework.txt");
        Files.writeString(testFile, "This is my homework.");

        Document result = manager.uploadAssignment("stu001", testFile);

        // Verify blob storage upload was called
        verify(blobStore).uploadFile(
                contains("students/stu001/"),
                eq(testFile),
                anyString());

        // Verify DocStore save was called with correct metadata
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(metadataService).saveDocument(docCaptor.capture());

        Document saved = docCaptor.getValue();
        assertEquals("stu001", saved.getStudentId());
        assertEquals("homework.txt", saved.getFileName());
        assertEquals(20, saved.getFileSizeBytes());
        assertNotNull(saved.getDocumentId());
        assertNotNull(saved.getUploadedAt());
        assertNotNull(result);
    }

    @Test
    void downloadAssignment_shouldLookupMetadataThenDownload() {
        Document doc = new Document();
        doc.setStudentId("stu001");
        doc.setDocumentId("doc123");
        doc.setS3Key("students/stu001/doc123_homework.txt");

        when(metadataService.getDocument("stu001", "doc123")).thenReturn(doc);
        when(blobStore.downloadFile("students/stu001/doc123_homework.txt"))
                .thenReturn("file content".getBytes());

        byte[] result = manager.downloadAssignment("stu001", "doc123");

        assertArrayEquals("file content".getBytes(), result);
        verify(metadataService).getDocument("stu001", "doc123");
        verify(blobStore).downloadFile("students/stu001/doc123_homework.txt");
    }

    @Test
    void downloadAssignment_shouldThrowWhenDocumentNotFound() {
        when(metadataService.getDocument("stu001", "missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> manager.downloadAssignment("stu001", "missing"));
    }

    @Test
    void listAssignments_shouldDelegateToMetadataService() {
        Document doc = new Document();
        doc.setStudentId("stu001");
        doc.setFileName("essay.pdf");

        when(metadataService.listDocumentsByStudent("stu001")).thenReturn(List.of(doc));

        List<Document> results = manager.listAssignments("stu001");

        assertEquals(1, results.size());
        assertEquals("essay.pdf", results.get(0).getFileName());
    }

    @Test
    void deleteAssignment_shouldDeleteFromBothBlobStoreAndDocStore() {
        Document doc = new Document();
        doc.setStudentId("stu001");
        doc.setDocumentId("doc123");
        doc.setS3Key("students/stu001/doc123_homework.txt");

        when(metadataService.getDocument("stu001", "doc123")).thenReturn(doc);

        manager.deleteAssignment("stu001", "doc123");

        verify(blobStore).deleteFile("students/stu001/doc123_homework.txt");
        verify(metadataService).deleteDocument("stu001", "doc123");
    }

    @Test
    void deleteAssignment_shouldDoNothingWhenDocumentNotFound() {
        when(metadataService.getDocument("stu001", "missing")).thenReturn(null);

        manager.deleteAssignment("stu001", "missing");

        verify(blobStore, never()).deleteFile(anyString());
        verify(metadataService, never()).deleteDocument(anyString(), anyString());
    }
}
