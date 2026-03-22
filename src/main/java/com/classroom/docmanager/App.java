package com.classroom.docmanager;

import com.classroom.docmanager.config.SubstrateConfig;
import com.classroom.docmanager.model.Document;
import com.classroom.docmanager.service.BlobStoreService;
import com.classroom.docmanager.service.DocumentMetadataService;
import com.classroom.docmanager.service.StudentDocumentManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI for the Student Document Manager.
 *
 * Usage:
 *   java -jar student-doc-manager.jar [--blobProvider aws|gcp] [--tableProvider aws|gcp] [--tableName NAME] upload   <studentId> <filePath>
 *   java -jar student-doc-manager.jar [--blobProvider aws|gcp] [--tableProvider aws|gcp] [--tableName NAME] list     <studentId>
 *   java -jar student-doc-manager.jar [--blobProvider aws|gcp] [--tableProvider aws|gcp] [--tableName NAME] download <studentId> <documentId> <savePath>
 *   java -jar student-doc-manager.jar [--blobProvider aws|gcp] [--tableProvider aws|gcp] [--tableName NAME] delete   <studentId> <documentId>
 */
public class App {

    private static final String DEFAULT_PROVIDER = "aws";
    private static final String DEFAULT_BUCKET_NAME = "classroom-student-docs";
    private static final String DEFAULT_TABLE_NAME = "StudentDocuments";
    private static final String JAR = "java -jar student-doc-manager-1.0-SNAPSHOT.jar";

    public static void main(String[] args) throws IOException {
        // Parse --flags and separate positional args
        String blobProvider = DEFAULT_PROVIDER;
        String tableProvider = DEFAULT_PROVIDER;
        String tableName = DEFAULT_TABLE_NAME;
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--blobProvider".equals(args[i]) && i + 1 < args.length) {
                blobProvider = args[++i];
            } else if ("--tableProvider".equals(args[i]) && i + 1 < args.length) {
                tableProvider = args[++i];
            } else if ("--tableName".equals(args[i]) && i + 1 < args.length) {
                tableName = args[++i];
            } else {
                positional.add(args[i]);
            }
        }

        if (positional.isEmpty()) {
            printUsage();
            System.exit(1);
        }

        var blobStore = new BlobStoreService(
                SubstrateConfig.bucketClient(blobProvider, DEFAULT_BUCKET_NAME),
                DEFAULT_BUCKET_NAME);
        var metadataService = new DocumentMetadataService(SubstrateConfig.docStoreClient(tableProvider, tableName));
        var manager = new StudentDocumentManager(blobStore, metadataService);

        String command = positional.get(0).toLowerCase();

        switch (command) {
            case "upload": {
                requirePositional(positional, 3, "upload <studentId> <filePath>");
                String studentId = positional.get(1);
                Path filePath = Path.of(positional.get(2));

                if (!Files.exists(filePath)) {
                    System.err.println("File not found: " + filePath);
                    System.exit(1);
                }

                Document doc = manager.uploadAssignment(studentId, filePath);
                System.out.println("Uploaded! Document ID: " + doc.getDocumentId());
                break;
            }

            case "list": {
                requirePositional(positional, 2, "list <studentId>");
                String studentId = positional.get(1);

                List<Document> docs = manager.listAssignments(studentId);
                if (docs.isEmpty()) {
                    System.out.println("No documents found for student: " + studentId);
                } else {
                    System.out.printf("Documents for student %s:%n", studentId);
                    for (Document d : docs) {
                        System.out.printf("  [%s] %s (%d bytes) - uploaded %s%n",
                                d.getDocumentId(), d.getFileName(),
                                d.getFileSizeBytes(), d.getUploadedAt());
                    }
                }
                break;
            }

            case "download": {
                requirePositional(positional, 4, "download <studentId> <documentId> <savePath>");
                String studentId = positional.get(1);
                String docId = positional.get(2);
                Path savePath = Path.of(positional.get(3));

                byte[] data = manager.downloadAssignment(studentId, docId);
                Files.write(savePath, data);
                System.out.println("Downloaded to " + savePath);
                break;
            }

            case "delete": {
                requirePositional(positional, 3, "delete <studentId> <documentId>");
                String studentId = positional.get(1);
                String docId = positional.get(2);

                manager.deleteAssignment(studentId, docId);
                System.out.println("Deleted document " + docId + " for student " + studentId);
                break;
            }

            default: {
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
            }
        }

        // Force exit — SDK clients spawn background threads that keep the JVM alive
        System.exit(0);
    }

    private static void requirePositional(List<String> positional, int required, String usage) {
        if (positional.size() < required) {
            System.err.println("Usage: " + JAR + " [--blobProvider aws|gcp] [--tableProvider aws|gcp] [--tableName NAME] " + usage);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Student Document Manager");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --blobProvider   Blob storage provider: aws or gcp (default: aws)");
        System.out.println("  --tableProvider  DocStore provider: aws or gcp (default: aws)");
        System.out.println("  --tableName      DynamoDB/Firestore table name (default: StudentDocuments)");
        System.out.println();
        System.out.println("Commands:");
        System.out.printf("  %s [options] upload   <studentId> <filePath>%n", JAR);
        System.out.printf("  %s [options] list     <studentId>%n", JAR);
        System.out.printf("  %s [options] download <studentId> <documentId> <savePath>%n", JAR);
        System.out.printf("  %s [options] delete   <studentId> <documentId>%n", JAR);
    }
}
