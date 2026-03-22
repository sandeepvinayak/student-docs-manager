package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * High-level orchestrator that coordinates S3 (blobstore) and DynamoDB operations.
 * This is the main entry point for business logic.
 */
public class StudentDocumentManager {

    private static final Logger log = LoggerFactory.getLogger(StudentDocumentManager.class);

    private final BlobStoreService blobStore;
    private final DocumentMetadataService metadataService;

    public StudentDocumentManager(BlobStoreService blobStore, DocumentMetadataService metadataService) {
        this.blobStore = blobStore;
        this.metadataService = metadataService;
    }

    /**
     * Upload a student's assignment file:
     *  1. Upload the file to S3 under students/{studentId}/{uuid}_{filename}
     *  2. Save metadata to DynamoDB
     */
    public Document uploadAssignment(String studentId, Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        String documentId = UUID.randomUUID().toString();
        String s3Key = "students/%s/%s_%s".formatted(studentId, documentId, fileName);
        String contentType = Files.probeContentType(filePath);
        long fileSize = Files.size(filePath);

        // Step 1: Upload to S3
        blobStore.uploadFile(s3Key, filePath, contentType != null ? contentType : "application/octet-stream");

        // Step 2: Save metadata to DynamoDB
        Document doc = new Document();
        doc.setStudentId(studentId);
        doc.setDocumentId(documentId);
        doc.setFileName(fileName);
        doc.setS3Key(s3Key);
        doc.setFileSizeBytes(fileSize);
        doc.setContentType(contentType);
        doc.setUploadedAt(Instant.now().toString());

        metadataService.saveDocument(doc);

        log.info("Assignment uploaded successfully: {}", doc);
        return doc;
    }

    /** Download a student's document by looking up the S3 key in DynamoDB. */
    public byte[] downloadAssignment(String studentId, String documentId) {
        Document doc = metadataService.getDocument(studentId, documentId);
        if (doc == null) {
            throw new IllegalArgumentException(
                    "No document found for student=%s, doc=%s".formatted(studentId, documentId));
        }
        return blobStore.downloadFile(doc.getS3Key());
    }

    /** List all assignments for a student (metadata only). */
    public List<Document> listAssignments(String studentId) {
        return metadataService.listDocumentsByStudent(studentId);
    }

    /** Delete a student's document from both S3 and DynamoDB. */
    public void deleteAssignment(String studentId, String documentId) {
        Document doc = metadataService.getDocument(studentId, documentId);
        if (doc != null) {
            blobStore.deleteFile(doc.getS3Key());
            metadataService.deleteDocument(studentId, documentId);
            log.info("Deleted assignment: {}", doc);
        }
    }
}
