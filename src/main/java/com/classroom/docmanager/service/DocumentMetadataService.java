package com.classroom.docmanager.service;

import com.classroom.docmanager.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all interactions with DynamoDB for document metadata.
 * Uses the low-level DynamoDB client with raw attribute maps — no Enhanced Client or beans.
 */
public class DocumentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataService.class);
    private static final String TABLE_NAME = "StudentDocuments";

    private final DynamoDbClient dynamoDb;

    public DocumentMetadataService(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    /** Create the DynamoDB table if it doesn't already exist. */
    public void createTableIfNotExists() {
        try {
            dynamoDb.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
            log.info("Table '{}' already exists", TABLE_NAME);
        } catch (ResourceNotFoundException e) {
            dynamoDb.createTable(CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(
                            KeySchemaElement.builder().attributeName("studentId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("documentId").keyType(KeyType.RANGE).build())
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("studentId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("documentId").attributeType(ScalarAttributeType.S).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build());
            log.info("Created table '{}'", TABLE_NAME);
        }
    }

    /** Save or update document metadata. */
    public void saveDocument(Document doc) {
        Map<String, AttributeValue> item = toAttributeMap(doc);
        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());
        log.info("Saved metadata: {}", doc);
    }

    /** Get a single document by studentId + documentId. */
    public Document getDocument(String studentId, String documentId) {
        Map<String, AttributeValue> key = Map.of(
                "studentId", AttributeValue.builder().s(studentId).build(),
                "documentId", AttributeValue.builder().s(documentId).build());

        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return null;
        }
        return fromAttributeMap(response.item());
    }

    /** List all documents for a given student. */
    public List<Document> listDocumentsByStudent(String studentId) {
        Map<String, AttributeValue> expressionValues = Map.of(
                ":sid", AttributeValue.builder().s(studentId).build());

        QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("studentId = :sid")
                .expressionAttributeValues(expressionValues)
                .build());

        return response.items().stream()
                .map(DocumentMetadataService::fromAttributeMap)
                .toList();
    }

    /** Delete a document's metadata. */
    public void deleteDocument(String studentId, String documentId) {
        Map<String, AttributeValue> key = Map.of(
                "studentId", AttributeValue.builder().s(studentId).build(),
                "documentId", AttributeValue.builder().s(documentId).build());

        dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build());
        log.info("Deleted metadata for student={}, doc={}", studentId, documentId);
    }

    // --- Manual mapping between Document and DynamoDB attribute maps ---

    private static Map<String, AttributeValue> toAttributeMap(Document doc) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("studentId", AttributeValue.builder().s(doc.getStudentId()).build());
        item.put("documentId", AttributeValue.builder().s(doc.getDocumentId()).build());
        item.put("fileName", AttributeValue.builder().s(doc.getFileName()).build());
        item.put("s3Key", AttributeValue.builder().s(doc.getS3Key()).build());
        item.put("fileSizeBytes", AttributeValue.builder().n(String.valueOf(doc.getFileSizeBytes())).build());
        item.put("uploadedAt", AttributeValue.builder().s(doc.getUploadedAt()).build());
        if (doc.getContentType() != null) {
            item.put("contentType", AttributeValue.builder().s(doc.getContentType()).build());
        }
        return item;
    }

    private static Document fromAttributeMap(Map<String, AttributeValue> item) {
        Document doc = new Document();
        doc.setStudentId(item.get("studentId").s());
        doc.setDocumentId(item.get("documentId").s());
        doc.setFileName(item.get("fileName").s());
        doc.setS3Key(item.get("s3Key").s());
        doc.setFileSizeBytes(Long.parseLong(item.get("fileSizeBytes").n()));
        doc.setUploadedAt(item.get("uploadedAt").s());
        if (item.containsKey("contentType")) {
            doc.setContentType(item.get("contentType").s());
        }
        return doc;
    }
}
