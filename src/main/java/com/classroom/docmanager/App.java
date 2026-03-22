package com.classroom.docmanager;

import com.classroom.docmanager.config.AwsConfig;
import com.classroom.docmanager.model.Document;
import com.classroom.docmanager.service.BlobStoreService;
import com.classroom.docmanager.service.DocumentMetadataService;
import com.classroom.docmanager.service.StudentDocumentManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI for the Student Document Manager.
 *
 * Usage:
 *   java -jar student-doc-manager.jar upload   <studentId> <filePath>
 *   java -jar student-doc-manager.jar list     <studentId>
 *   java -jar student-doc-manager.jar download <studentId> <documentId> <savePath>
 *   java -jar student-doc-manager.jar delete   <studentId> <documentId>
 */
public class App {

    private static final String BUCKET_NAME = "classroom-student-docs";
    private static final String JAR = "java -jar student-doc-manager-1.0-SNAPSHOT.jar";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        var blobStore = new BlobStoreService(AwsConfig.s3Client(), BUCKET_NAME);
        var metadataService = new DocumentMetadataService(AwsConfig.dynamoDbClient());
        var manager = new StudentDocumentManager(blobStore, metadataService);

        String command = args[0].toLowerCase();

        switch (command) {
            case "upload" -> {
                requireArgs(args, 3, "upload <studentId> <filePath>");
                String studentId = args[1];
                Path filePath = Path.of(args[2]);

                if (!Files.exists(filePath)) {
                    System.err.println("File not found: " + filePath);
                    System.exit(1);
                }

                Document doc = manager.uploadAssignment(studentId, filePath);
                System.out.println("Uploaded! Document ID: " + doc.getDocumentId());
            }

            case "list" -> {
                requireArgs(args, 2, "list <studentId>");
                String studentId = args[1];

                List<Document> docs = manager.listAssignments(studentId);
                if (docs.isEmpty()) {
                    System.out.println("No documents found for student: " + studentId);
                } else {
                    System.out.printf("Documents for student %s:%n", studentId);
                    docs.forEach(d -> System.out.printf(
                            "  [%s] %s (%d bytes) - uploaded %s%n",
                            d.getDocumentId(), d.getFileName(),
                            d.getFileSizeBytes(), d.getUploadedAt()));
                }
            }

            case "download" -> {
                requireArgs(args, 4, "download <studentId> <documentId> <savePath>");
                String studentId = args[1];
                String docId = args[2];
                Path savePath = Path.of(args[3]);

                byte[] data = manager.downloadAssignment(studentId, docId);
                Files.write(savePath, data);
                System.out.println("Downloaded to " + savePath);
            }

            case "delete" -> {
                requireArgs(args, 3, "delete <studentId> <documentId>");
                String studentId = args[1];
                String docId = args[2];

                manager.deleteAssignment(studentId, docId);
                System.out.println("Deleted document " + docId + " for student " + studentId);
            }

            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
            }
        }
    }

    private static void requireArgs(String[] args, int required, String usage) {
        if (args.length < required) {
            System.err.println("Usage: " + JAR + " " + usage);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Student Document Manager");
        System.out.println();
        System.out.println("Usage:");
        System.out.printf("  %s upload   <studentId> <filePath>%n", JAR);
        System.out.printf("  %s list     <studentId>%n", JAR);
        System.out.printf("  %s download <studentId> <documentId> <savePath>%n", JAR);
        System.out.printf("  %s delete   <studentId> <documentId>%n", JAR);
    }
}
