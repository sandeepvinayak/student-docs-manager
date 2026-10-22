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
 * Handles all interactions with the document store for document metadata
 * (substrate-neutral via the Substrate SDK DocStoreClient).
 * Uses raw field maps — no beans or annotations.
 */
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocStoreClient docStore;

    public DocumentMetadataService(DocStoreClient docStore) {
        this.docStore = docStore;
    }

    /** Save or update document metadata. */
    public void saveDocument(Document doc) {
        docStore.put(new com.salesforce.multicloudj.docstore.driver.Document(toFieldMap(doc)));
        log.info("Saved metadata: {}", doc);
    }

    /** Get a single document by studentId + documentId, or null if it does not exist. */
    public Document getDocument(String studentId, String documentId) {
        Map<String, Object> fields = keyMap(studentId, documentId);
        try {
            docStore.get(new com.salesforce.multicloudj.docstore.driver.Document(fields));
        } catch (ResourceNotFoundException e) {
            return null;
        }

        // Some substrates leave the input untouched instead of throwing when the key is missing
        if (!fields.containsKey("s3Key")) {
            return null;
        }
        return fromFieldMap(fields);
    }

    /** List all documents for a given student. */
    public List<Document> listDocumentsByStudent(String studentId) {
        DocumentIterator results = docStore.query()
                .where("studentId", FilterOperation.EQUAL, studentId)
                .get();

        List<Document> docs = new ArrayList<>();
        while (results.hasNext()) {
            Map<String, Object> fields = new HashMap<>();
            results.next(new com.salesforce.multicloudj.docstore.driver.Document(fields));
            docs.add(fromFieldMap(fields));
        }
        return docs;
    }

    /** Delete a document's metadata. */
    public void deleteDocument(String studentId, String documentId) {
        docStore.delete(new com.salesforce.multicloudj.docstore.driver.Document(keyMap(studentId, documentId)));
        log.info("Deleted metadata for student={}, doc={}", studentId, documentId);
    }

    // --- Manual mapping between Document and docstore field maps ---

    private static Map<String, Object> keyMap(String studentId, String documentId) {
        Map<String, Object> key = new HashMap<>();
        key.put("studentId", studentId);
        key.put("documentId", documentId);
        return key;
    }

    private static Map<String, Object> toFieldMap(Document doc) {
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

    private static Document fromFieldMap(Map<String, Object> item) {
        Document doc = new Document();
        doc.setStudentId((String) item.get("studentId"));
        doc.setDocumentId((String) item.get("documentId"));
        doc.setFileName((String) item.get("fileName"));
        doc.setS3Key((String) item.get("s3Key"));
        doc.setFileSizeBytes(((Number) item.get("fileSizeBytes")).longValue());
        doc.setUploadedAt((String) item.get("uploadedAt"));
        if (item.get("contentType") != null) {
            doc.setContentType((String) item.get("contentType"));
        }
        return doc;
    }
}
