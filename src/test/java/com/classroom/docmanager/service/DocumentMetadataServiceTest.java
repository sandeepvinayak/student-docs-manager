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

    private static com.salesforce.multicloudj.docstore.driver.Document anyDoc() {
        return any(com.salesforce.multicloudj.docstore.driver.Document.class);
    }

    @Test
    void saveDocument_shouldPutPojo() {
        Document doc = new Document("stu001", "doc1", "a.txt", "students/stu001/doc1_a.txt", 10, "text/plain", "now");

        service.saveDocument(doc);

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStore).put(captor.capture());
        assertEquals("stu001", captor.getValue().getField("studentId"));
        assertEquals("doc1", captor.getValue().getField("documentId"));
        assertEquals("students/stu001/doc1_a.txt", captor.getValue().getField("s3Key"));
    }

    @Test
    void getDocument_shouldReturnPopulatedDocument() {
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            assertEquals("stu001", d.getField("studentId"));
            assertEquals("doc1", d.getField("documentId"));
            d.setField("s3Key", "students/stu001/doc1_a.txt");
            d.setField("fileName", "a.txt");
            return null;
        }).when(docStore).get(anyDoc());

        Document result = service.getDocument("stu001", "doc1");

        assertNotNull(result);
        assertEquals("students/stu001/doc1_a.txt", result.getS3Key());
        assertEquals("a.txt", result.getFileName());
    }

    @Test
    void getDocument_shouldReturnNullWhenMissing() {
        // Substrate leaves the document unpopulated when the key does not exist.
        assertNull(service.getDocument("stu001", "missing"));
        verify(docStore).get(anyDoc());
    }

    @Test
    void listDocumentsByStudent_shouldQueryByPartitionKey() {
        Query query = mock(Query.class);
        DocumentIterator iterator = mock(DocumentIterator.class);
        when(docStore.query()).thenReturn(query);
        when(query.where(anyString(), any(FilterOperation.class), any())).thenReturn(query);
        when(query.get()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, true, false);
        doAnswer(inv -> {
            com.salesforce.multicloudj.docstore.driver.Document d = inv.getArgument(0);
            d.setField("studentId", "stu001");
            d.setField("fileName", "file.txt");
            return null;
        }).when(iterator).next(anyDoc());

        List<Document> results = service.listDocumentsByStudent("stu001");

        assertEquals(2, results.size());
        assertEquals("file.txt", results.get(0).getFileName());
        verify(query).where("studentId", FilterOperation.EQUAL, "stu001");
        verify(iterator).stop();
    }

    @Test
    void deleteDocument_shouldDeleteByKey() {
        service.deleteDocument("stu001", "doc1");

        ArgumentCaptor<com.salesforce.multicloudj.docstore.driver.Document> captor =
                ArgumentCaptor.forClass(com.salesforce.multicloudj.docstore.driver.Document.class);
        verify(docStore).delete(captor.capture());
        assertEquals("stu001", captor.getValue().getField("studentId"));
        assertEquals("doc1", captor.getValue().getField("documentId"));
    }
}
