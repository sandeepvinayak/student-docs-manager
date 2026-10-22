package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
import com.salesforce.multicloudj.common.exceptions.ResourceNotFoundException;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.client.Query;
import com.salesforce.multicloudj.docstore.driver.DocumentIterator;
import com.salesforce.multicloudj.docstore.driver.FilterOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentMetadataServiceTest {

    private DocStoreClient docStore;
    private DocumentMetadataService service;

    @BeforeEach
    void setUp() {
        docStore = mock(DocStoreClient.class);
        service = new DocumentMetadataService(docStore);
    }

    @Test
    void saveDocument_shouldPutAllFields() {
        Document doc = new Document("stu001", "doc123", "homework.txt",
                "students/stu001/doc123_homework.txt", 20, "text/plain", "2026-10-21T00:00:00Z");

        service.saveDocument(doc);

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStore).put(captor.capture());
        var saved = captor.getValue();
        assertEquals("stu001", saved.getField("studentId"));
        assertEquals("doc123", saved.getField("documentId"));
        assertEquals("homework.txt", saved.getField("fileName"));
        assertEquals("students/stu001/doc123_homework.txt", saved.getField("s3Key"));
        assertEquals(20L, saved.getField("fileSizeBytes"));
        assertEquals("text/plain", saved.getField("contentType"));
        assertEquals("2026-10-21T00:00:00Z", saved.getField("uploadedAt"));
    }

    @Test
    void saveDocument_shouldOmitNullContentType() {
        Document doc = new Document("stu001", "doc123", "f", "k", 1, null, "t");

        service.saveDocument(doc);

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStore).put(captor.capture());
        assertFalse(captor.getValue().hasField("contentType"));
    }

    @Test
    void getDocument_shouldPopulateFromStore() {
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            d.setField("fileName", "homework.txt");
            d.setField("s3Key", "students/stu001/doc123_homework.txt");
            d.setField("fileSizeBytes", 20L);
            d.setField("uploadedAt", "2026-10-21T00:00:00Z");
            return null;
        }).when(docStore).get(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        Document doc = service.getDocument("stu001", "doc123");

        assertNotNull(doc);
        assertEquals("stu001", doc.getStudentId());
        assertEquals("doc123", doc.getDocumentId());
        assertEquals("students/stu001/doc123_homework.txt", doc.getS3Key());
        assertEquals(20, doc.getFileSizeBytes());
        assertNull(doc.getContentType());
    }

    @Test
    void getDocument_shouldReturnNullWhenStoreLeavesDocumentUnpopulated() {
        assertNull(service.getDocument("stu001", "missing"));
    }

    @Test
    void getDocument_shouldReturnNullWhenStoreThrowsNotFound() {
        doThrow(new ResourceNotFoundException("missing"))
                .when(docStore).get(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        assertNull(service.getDocument("stu001", "missing"));
    }

    @Test
    void listDocumentsByStudent_shouldQueryByPartitionKey() {
        Query query = mock(Query.class);
        DocumentIterator results = mock(DocumentIterator.class);
        when(docStore.query()).thenReturn(query);
        when(query.where("studentId", FilterOperation.EQUAL, "stu001")).thenReturn(query);
        when(query.get()).thenReturn(results);
        when(results.hasNext()).thenReturn(true, false);
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            d.setField("studentId", "stu001");
            d.setField("documentId", "doc123");
            d.setField("fileName", "essay.pdf");
            d.setField("s3Key", "k");
            d.setField("fileSizeBytes", 25.0);
            d.setField("uploadedAt", "t");
            return null;
        }).when(results).next(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        List<Document> docs = service.listDocumentsByStudent("stu001");

        assertEquals(1, docs.size());
        assertEquals("essay.pdf", docs.get(0).getFileName());
        assertEquals(25, docs.get(0).getFileSizeBytes());
    }

    @Test
    void deleteDocument_shouldDeleteByKey() {
        service.deleteDocument("stu001", "doc123");

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStore).delete(captor.capture());
        assertEquals("stu001", captor.getValue().getField("studentId"));
        assertEquals("doc123", captor.getValue().getField("documentId"));
    }
}
