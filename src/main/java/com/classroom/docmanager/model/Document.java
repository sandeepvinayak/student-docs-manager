package com.classroom.docmanager.model;

/**
 * Plain POJO representing a student document's metadata.
 * No framework annotations — manual mapping to/from DynamoDB attribute maps.
 */
public class Document {

    private String studentId;
    private String documentId;
    private String fileName;
    private String s3Key;
    private long fileSizeBytes;
    private String contentType;
    private String uploadedAt;

    public Document() {}

    public Document(String studentId, String documentId, String fileName,
                    String s3Key, long fileSizeBytes, String contentType, String uploadedAt) {
        this.studentId = studentId;
        this.documentId = documentId;
        this.fileName = fileName;
        this.s3Key = s3Key;
        this.fileSizeBytes = fileSizeBytes;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }

    @Override
    public String toString() {
        return "Document{studentId='%s', documentId='%s', fileName='%s', size=%d}"
                .formatted(studentId, documentId, fileName, fileSizeBytes);
    }
}
