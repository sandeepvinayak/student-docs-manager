package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
import com.salesforce.multicloudj.common.exceptions.ResourceNotFoundException;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.DocumentIterator;
import com.salesforce.multicloudj.docstore.driver.FilterOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all interactions with the document store (Substrate SDK DocStoreClient) for document metadata.
 * The table name and key schema (studentId / documentId) are configured on the DocStoreClient
 * via CollectionOptions; the {@link Document} POJO is mapped directly by the Substrate codec.
 */
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocStoreClient docStore;

    public DocumentMetadataService(DocStoreClient docStore) {
        this.docStore = docStore;
    }

    /** Save or update document metadata (unconditional upsert). */
    public void saveDocument(Document doc) {
        docStore.put(wrap(doc));
        log.info("Saved metadata: {}", doc);
    }

    /** Get a single document by studentId + documentId, or null if it does not exist. */
    public Document getDocument(String studentId, String documentId) {
        Document probe = keyOf(studentId, documentId);
        try {
            docStore.get(wrap(probe));
        } catch (ResourceNotFoundException e) {
            return null;
        }
        // A missing item leaves the probe unpopulated; s3Key is always written by saveDocument.
        return probe.getS3Key() == null ? null : probe;
    }

    /** List all documents for a given student. */
    public List<Document> listDocumentsByStudent(String studentId) {
        DocumentIterator it = docStore.query()
                .where("studentId", FilterOperation.EQUAL, studentId)
                .get();

        List<Document> results = new ArrayList<>();
        try {
            while (it.hasNext()) {
                Document doc = new Document();
                it.next(wrap(doc));
                results.add(doc);
            }
        } finally {
            it.stop();
        }
        return results;
    }

    /** Delete a document's metadata. */
    public void deleteDocument(String studentId, String documentId) {
        docStore.delete(wrap(keyOf(studentId, documentId)));
        log.info("Deleted metadata for student={}, doc={}", studentId, documentId);
    }

    private static Document keyOf(String studentId, String documentId) {
        Document doc = new Document();
        doc.setStudentId(studentId);
        doc.setDocumentId(documentId);
        return doc;
    }

    private static com.salesforce.multicloudj.docstore.driver.Document wrap(Document doc) {
        return new com.salesforce.multicloudj.docstore.driver.Document(doc);
    }
}
