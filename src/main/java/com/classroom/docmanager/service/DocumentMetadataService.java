package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
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
 * Handles all interactions with DocStore for document metadata.
 * Uses the Substrate SDK DocStoreClient for cloud-agnostic document storage.
 */
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocStoreClient docStoreClient;

    public DocumentMetadataService(DocStoreClient docStoreClient) {
        this.docStoreClient = docStoreClient;
    }

    /** Save or update document metadata. */
    public void saveDocument(Document doc) {
        Map<String, Object> item = toMap(doc);
        com.salesforce.multicloudj.docstore.driver.Document dsDoc =
                new com.salesforce.multicloudj.docstore.driver.Document(item);
        docStoreClient.put(dsDoc);
        log.info("Saved metadata: {}", doc);
    }

    /** Get a single document by studentId + documentId. */
    public Document getDocument(String studentId, String documentId) {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("studentId", studentId);
        keyMap.put("documentId", documentId);
        com.salesforce.multicloudj.docstore.driver.Document dsDoc =
                new com.salesforce.multicloudj.docstore.driver.Document(keyMap);

        try {
            docStoreClient.get(dsDoc);
        } catch (Exception e) {
            return null;
        }

        return fromDocStoreDocument(dsDoc);
    }

    /** List all documents for a given student. */
    public List<Document> listDocumentsByStudent(String studentId) {
        DocumentIterator iterator = docStoreClient.query()
                .where("studentId", FilterOperation.EQUAL, studentId)
                .get();

        List<Document> results = new ArrayList<>();
        while (iterator.hasNext()) {
            Map<String, Object> item = new HashMap<>();
            com.salesforce.multicloudj.docstore.driver.Document dsDoc =
                    new com.salesforce.multicloudj.docstore.driver.Document(item);
            iterator.next(dsDoc);
            results.add(fromMap(item));
        }
        return results;
    }

    /** Delete a document's metadata. */
    public void deleteDocument(String studentId, String documentId) {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("studentId", studentId);
        keyMap.put("documentId", documentId);
        com.salesforce.multicloudj.docstore.driver.Document dsDoc =
                new com.salesforce.multicloudj.docstore.driver.Document(keyMap);
        docStoreClient.delete(dsDoc);
        log.info("Deleted metadata for student={}, doc={}", studentId, documentId);
    }

    // --- Mapping between Document POJO and Map ---

    private static Map<String, Object> toMap(Document doc) {
        Map<String, Object> item = new HashMap<>();
        item.put("studentId", doc.getStudentId());
        item.put("documentId", doc.getDocumentId());
        item.put("fileName", doc.getFileName());
        item.put("s3Key", doc.getS3Key());
        item.put("fileSizeBytes", doc.getFileSizeBytes());
        item.put("uploadedAt", doc.getUploadedAt());
        if (doc.getContentType() != null) {
            item.put("contentType", doc.getContentType());
        }
        return item;
    }

    private static Document fromDocStoreDocument(com.salesforce.multicloudj.docstore.driver.Document dsDoc) {
        Document doc = new Document();
        doc.setStudentId((String) dsDoc.getField("studentId"));
        doc.setDocumentId((String) dsDoc.getField("documentId"));
        doc.setFileName((String) dsDoc.getField("fileName"));
        doc.setS3Key((String) dsDoc.getField("s3Key"));
        Object fileSizeObj = dsDoc.getField("fileSizeBytes");
        doc.setFileSizeBytes(fileSizeObj instanceof Number ? ((Number) fileSizeObj).longValue() : Long.parseLong(fileSizeObj.toString()));
        doc.setUploadedAt((String) dsDoc.getField("uploadedAt"));
        Object contentType = dsDoc.getField("contentType");
        if (contentType != null) {
            doc.setContentType(contentType.toString());
        }
        return doc;
    }

    private static Document fromMap(Map<String, Object> item) {
        Document doc = new Document();
        doc.setStudentId((String) item.get("studentId"));
        doc.setDocumentId((String) item.get("documentId"));
        doc.setFileName((String) item.get("fileName"));
        doc.setS3Key((String) item.get("s3Key"));
        Object fileSizeObj = item.get("fileSizeBytes");
        if (fileSizeObj != null) {
            doc.setFileSizeBytes(fileSizeObj instanceof Number ? ((Number) fileSizeObj).longValue() : Long.parseLong(fileSizeObj.toString()));
        }
        doc.setUploadedAt((String) item.get("uploadedAt"));
        Object contentType = item.get("contentType");
        if (contentType != null) {
            doc.setContentType(contentType.toString());
        }
        return doc;
    }
}
