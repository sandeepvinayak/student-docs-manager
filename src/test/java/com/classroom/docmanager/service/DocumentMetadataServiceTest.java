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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentMetadataServiceTest {

    private DocStoreClient docStoreClient;
    private DocumentMetadataService service;

    @BeforeEach
    void setUp() {
        docStoreClient = mock(DocStoreClient.class);
        service = new DocumentMetadataService(docStoreClient);
    }

    private static Document sampleDoc(String docId, String contentType) {
        return new Document("stu001", docId, "homework.txt",
                "students/stu001/" + docId + "_homework.txt", 20L, contentType, "2026-01-01T00:00:00Z");
    }

    @Test
    void saveDocument_shouldPutAllFields() {
        service.saveDocument(sampleDoc("doc123", "text/plain"));

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStoreClient).put(captor.capture());
        var stored = captor.getValue();
        assertEquals("stu001", stored.getField("studentId"));
        assertEquals("doc123", stored.getField("documentId"));
        assertEquals("homework.txt", stored.getField("fileName"));
        assertEquals("students/stu001/doc123_homework.txt", stored.getField("s3Key"));
        assertEquals(20L, stored.getField("fileSizeBytes"));
        assertEquals("text/plain", stored.getField("contentType"));
        assertEquals("2026-01-01T00:00:00Z", stored.getField("uploadedAt"));
    }

    @Test
    void saveDocument_shouldOmitNullContentType() {
        service.saveDocument(sampleDoc("doc123", null));

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStoreClient).put(captor.capture());
        assertFalse(captor.getValue().hasField("contentType"));
    }

    @Test
    void getDocument_shouldReturnPopulatedDocument() {
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            assertEquals("stu001", d.getField("studentId"));
            assertEquals("doc123", d.getField("documentId"));
            DocumentMetadataService.toMap(sampleDoc("doc123", "text/plain")).forEach(d::setField);
            return null;
        }).when(docStoreClient).get(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        Document result = service.getDocument("stu001", "doc123");

        assertNotNull(result);
        assertEquals("students/stu001/doc123_homework.txt", result.getS3Key());
        assertEquals(20L, result.getFileSizeBytes());
        assertEquals("text/plain", result.getContentType());
    }

    @Test
    void getDocument_shouldReturnNullWhenItemNotPopulated() {
        // get() leaves the document untouched when the item does not exist
        assertNull(service.getDocument("stu001", "missing"));
    }

    @Test
    void getDocument_shouldReturnNullOnResourceNotFound() {
        doThrow(new ResourceNotFoundException("not found"))
                .when(docStoreClient).get(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        assertNull(service.getDocument("stu001", "missing"));
    }

    @Test
    void listDocumentsByStudent_shouldQueryByPartitionKeyAndMapResults() {
        Query query = mock(Query.class);
        DocumentIterator iterator = mock(DocumentIterator.class);
        when(docStoreClient.query()).thenReturn(query);
        when(query.where("studentId", FilterOperation.EQUAL, "stu001")).thenReturn(query);
        when(query.get()).thenReturn(iterator);

        List<Map<String, Object>> rows = List.of(
                DocumentMetadataService.toMap(sampleDoc("doc1", "text/plain")),
                DocumentMetadataService.toMap(sampleDoc("doc2", null)));
        int[] idx = {0};
        when(iterator.hasNext()).thenAnswer(inv -> idx[0] < rows.size());
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            rows.get(idx[0]++).forEach(d::setField);
            return null;
        }).when(iterator).next(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        List<Document> results = service.listDocumentsByStudent("stu001");

        assertEquals(2, results.size());
        assertEquals("doc1", results.get(0).getDocumentId());
        assertEquals("doc2", results.get(1).getDocumentId());
        assertNull(results.get(1).getContentType());
        verify(iterator).stop();
    }

    @Test
    void deleteDocument_shouldDeleteByCompositeKey() {
        service.deleteDocument("stu001", "doc123");

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStoreClient).delete(captor.capture());
        assertEquals("stu001", captor.getValue().getField("studentId"));
        assertEquals("doc123", captor.getValue().getField("documentId"));
    }
}
