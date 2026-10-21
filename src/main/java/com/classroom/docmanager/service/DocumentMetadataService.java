package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.DocumentIterator;
import com.salesforce.multicloudj.docstore.driver.FilterOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all interactions with the document store for document metadata.
 * Uses the Substrate SDK DocStoreClient; the Document POJO's field names map 1:1 to the stored
 * attributes (partition key: studentId, sort key: documentId).
 */
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);

    public static final String TABLE_NAME = "StudentDocuments";
    public static final String PARTITION_KEY = "studentId";
    public static final String SORT_KEY = "documentId";

    private final DocStoreClient docStore;

    public DocumentMetadataService(DocStoreClient docStore) {
        this.docStore = docStore;
    }

    /** Save or update document metadata. */
    public void saveDocument(Document doc) {
        docStore.put(new com.salesforce.multicloudj.docstore.driver.Document(doc));
        log.info("Saved metadata: {}", doc);
    }

    /** Get a single document by studentId + documentId. */
    public Document getDocument(String studentId, String documentId) {
        Document doc = keyOnly(studentId, documentId);
        docStore.get(new com.salesforce.multicloudj.docstore.driver.Document(doc));

        // get() leaves the input untouched when no item matches the key
        if (doc.getUploadedAt() == null) {
            return null;
        }
        return doc;
    }

    /** List all documents for a given student. */
    public List<Document> listDocumentsByStudent(String studentId) {
        DocumentIterator it = docStore.query()
                .where(PARTITION_KEY, FilterOperation.EQUAL, studentId)
                .get();

        List<Document> results = new ArrayList<>();
        while (it.hasNext()) {
            Document doc = new Document();
            it.next(new com.salesforce.multicloudj.docstore.driver.Document(doc));
            results.add(doc);
        }
        return results;
    }

    /** Delete a document's metadata. */
    public void deleteDocument(String studentId, String documentId) {
        docStore.delete(new com.salesforce.multicloudj.docstore.driver.Document(keyOnly(studentId, documentId)));
        log.info("Deleted metadata for student={}, doc={}", studentId, documentId);
    }

    private static Document keyOnly(String studentId, String documentId) {
        Document doc = new Document();
        doc.setStudentId(studentId);
        doc.setDocumentId(documentId);
        return doc;
    }
}
