package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
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
    void saveDocument_shouldPutPojo() {
        Document doc = new Document("stu001", "doc123", "homework.txt",
                "students/stu001/doc123_homework.txt", 20, "text/plain", "2026-10-21T00:00:00Z");

        service.saveDocument(doc);

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStore).put(captor.capture());
        assertEquals("stu001", captor.getValue().getField("studentId"));
        assertEquals("doc123", captor.getValue().getField("documentId"));
        assertEquals(20L, captor.getValue().getField("fileSizeBytes"));
    }

    @Test
    void getDocument_shouldReturnPopulatedDocument() {
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            assertEquals("stu001", d.getField("studentId"));
            assertEquals("doc123", d.getField("documentId"));
            d.setField("fileName", "homework.txt");
            d.setField("s3Key", "students/stu001/doc123_homework.txt");
            d.setField("uploadedAt", "2026-10-21T00:00:00Z");
            return null;
        }).when(docStore).get(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        Document result = service.getDocument("stu001", "doc123");

        assertNotNull(result);
        assertEquals("homework.txt", result.getFileName());
        assertEquals("students/stu001/doc123_homework.txt", result.getS3Key());
    }

    @Test
    void getDocument_shouldReturnNullWhenNotFound() {
        // DocStoreClient.get leaves the input document untouched when the key doesn't exist
        assertNull(service.getDocument("stu001", "missing"));
    }

    @Test
    void listDocumentsByStudent_shouldQueryByPartitionKey() {
        Query query = mock(Query.class);
        DocumentIterator it = mock(DocumentIterator.class);
        when(docStore.query()).thenReturn(query);
        when(query.where("studentId", FilterOperation.EQUAL, "stu001")).thenReturn(query);
        when(query.get()).thenReturn(it);
        when(it.hasNext()).thenReturn(true, true, false);
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            d.setField("studentId", "stu001");
            d.setField("fileName", "essay.pdf");
            return null;
        }).when(it).next(any(com.salesforce.multicloudj.docstore.driver.Document.class));

        List<Document> results = service.listDocumentsByStudent("stu001");

        assertEquals(2, results.size());
        assertNotSame(results.get(0), results.get(1));
        assertEquals("essay.pdf", results.get(0).getFileName());
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
