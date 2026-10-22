package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
import com.salesforce.multicloudj.common.exceptions.ResourceNotFoundException;
import com.salesforce.multicloudj.docstore.client.DocStoreClient;
import com.salesforce.multicloudj.docstore.driver.DocumentIterator;
import com.salesforce.multicloudj.docstore.driver.FilterOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all interactions with the document store for document metadata via the
 * Substrate SDK {@link DocStoreClient}. The client is bound to the "StudentDocuments"
 * collection (partition key studentId, sort key documentId) at construction time.
 */
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocStoreClient docStore;

    public DocumentMetadataService(DocStoreClient docStore) {
        this.docStore = docStore;
    }

    /** Save or update document metadata (unconditional upsert). */
    public void saveDocument(Document doc) {
        docStore.put(new com.salesforce.multicloudj.docstore.driver.Document(toMap(doc)));
        log.info("Saved metadata: {}", doc);
    }

    /** Get a single document by studentId + documentId, or null if it does not exist. */
    public Document getDocument(String studentId, String documentId) {
        Map<String, Object> item = keyMap(studentId, documentId);
        try {
            docStore.get(new com.salesforce.multicloudj.docstore.driver.Document(item));
        } catch (ResourceNotFoundException e) {
            return null;
        }
        // A missing item leaves the supplied document populated with only its key fields.
        if (!item.containsKey("s3Key")) {
            return null;
        }
        return fromMap(item);
    }

    /** List all documents for a given student (partition-key query; paginates automatically). */
    public List<Document> listDocumentsByStudent(String studentId) {
        DocumentIterator it = docStore.query()
                .where("studentId", FilterOperation.EQUAL, studentId)
                .get();

        List<Document> results = new ArrayList<>();
        try {
            while (it.hasNext()) {
                Map<String, Object> item = new HashMap<>();
                it.next(new com.salesforce.multicloudj.docstore.driver.Document(item));
                results.add(fromMap(item));
            }
        } finally {
            it.stop();
        }
        return results;
    }

    /** Delete a document's metadata. */
    public void deleteDocument(String studentId, String documentId) {
        docStore.delete(new com.salesforce.multicloudj.docstore.driver.Document(keyMap(studentId, documentId)));
        log.info("Deleted metadata for student={}, doc={}", studentId, documentId);
    }

    // --- Mapping between Document and docstore field maps ---

    private static Map<String, Object> keyMap(String studentId, String documentId) {
        Map<String, Object> key = new HashMap<>();
        key.put("studentId", studentId);
        key.put("documentId", documentId);
        return key;
    }

    static Map<String, Object> toMap(Document doc) {
        Map<String, Object> item = keyMap(doc.getStudentId(), doc.getDocumentId());
        item.put("fileName", doc.getFileName());
        item.put("s3Key", doc.getS3Key());
        item.put("fileSizeBytes", doc.getFileSizeBytes());
        item.put("uploadedAt", doc.getUploadedAt());
        if (doc.getContentType() != null) {
            item.put("contentType", doc.getContentType());
        }
        return item;
    }

    static Document fromMap(Map<String, Object> item) {
        Document doc = new Document();
        doc.setStudentId((String) item.get("studentId"));
        doc.setDocumentId((String) item.get("documentId"));
        doc.setFileName((String) item.get("fileName"));
        doc.setS3Key((String) item.get("s3Key"));
        Object size = item.get("fileSizeBytes");
        if (size instanceof Number n) {
            doc.setFileSizeBytes(n.longValue());
        } else if (size != null) {
            doc.setFileSizeBytes(Long.parseLong(size.toString()));
        }
        doc.setUploadedAt((String) item.get("uploadedAt"));
        if (item.get("contentType") != null) {
            doc.setContentType((String) item.get("contentType"));
        }
        return doc;
    }
}
