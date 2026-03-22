# Student Document Manager

A small classroom project that demonstrates **Amazon S3** (blobstore) and **Amazon DynamoDB** integration in Java using AWS SDK v2. Upload student assignment files to S3 and track metadata in DynamoDB.

## How It Works

- **S3** stores files under `students/{studentId}/{uuid}_{filename}`
- **DynamoDB** table `StudentDocuments` stores metadata (partition key: `studentId`, sort key: `documentId`)

## Prerequisites

You need the following set up **before** running the app.

### Step 1: Java 17 and Maven

```bash
export JAVA_HOME=/Users/sandeeppal/sfdc-java/openjdk_17.0.17.0.101_17.63.12_aarch64
java -version
mvn -version
```

### Step 2: AWS Credentials

Configure credentials via one of:
- Environment variables: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
- Shared credentials file: `~/.aws/credentials`
- IAM role (EC2/ECS)

### Step 3: Create the S3 Bucket

```bash
aws s3 mb s3://classroom-student-docs --region us-west-2
```

### Step 4: Create the DynamoDB Table

```bash
aws dynamodb create-table \
  --table-name StudentDocuments \
  --attribute-definitions \
    AttributeName=studentId,AttributeType=S \
    AttributeName=documentId,AttributeType=S \
  --key-schema \
    AttributeName=studentId,KeyType=HASH \
    AttributeName=documentId,KeyType=RANGE \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region us-west-2
```

### Step 5: IAM Permissions

Your credentials need:
- **S3**: `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, `s3:ListBucket`
- **DynamoDB**: `dynamodb:PutItem`, `dynamodb:GetItem`, `dynamodb:Query`, `dynamodb:DeleteItem`

## Build

```bash
# Run tests
mvn clean test

# Build the fat JAR
mvn clean package
```

## Run the Demo

### Setup

```bash
JAR=target/student-doc-manager-1.0-SNAPSHOT.jar
```

Each command is a single line you can copy-paste:

```
java -jar $JAR upload   <studentId> <filePath>
java -jar $JAR list     <studentId>
java -jar $JAR download <studentId> <documentId> <savePath>
java -jar $JAR delete   <studentId> <documentId>
```

### Step 1 — Create sample files

```bash
echo "This is my homework assignment." > /tmp/homework.txt
echo "Essay on cloud computing." > /tmp/essay.txt
```

### Step 2 — Upload

```bash
java -jar $JAR upload stu001 /tmp/homework.txt
# Uploaded! Document ID: a3f1b2c4-...

java -jar $JAR upload stu001 /tmp/essay.txt
# Uploaded! Document ID: b7e2c3d5-...
```

### Step 3 — List

```bash
java -jar $JAR list stu001
# Documents for student stu001:
#   [a3f1b2c4-...] homework.txt (31 bytes) - uploaded 2026-03-22T17:30:00Z
#   [b7e2c3d5-...] essay.txt (25 bytes) - uploaded 2026-03-22T17:31:00Z
```

### Step 4 — Download

Copy a document ID from the list output and use it here:

```bash
java -jar $JAR download stu001 <documentId> /tmp/downloaded-homework.txt
# Downloaded to /tmp/downloaded-homework.txt

cat /tmp/downloaded-homework.txt
# This is my homework assignment.
```

### Step 5 — Delete

```bash
java -jar $JAR delete stu001 <documentId>
# Deleted document <documentId> for student stu001
```

### Step 6 — Verify deletion

```bash
java -jar $JAR list stu001
# Documents for student stu001:
#   [b7e2c3d5-...] essay.txt (25 bytes) - uploaded 2026-03-22T17:31:00Z
```

The homework file is gone from both S3 and DynamoDB. Only the essay remains.

## Project Structure

```
src/main/java/com/classroom/docmanager/
├── App.java                              # CLI entry point (parses args, runs commands)
├── config/
│   └── AwsConfig.java                    # AWS client factory (S3, DynamoDB)
├── model/
│   └── Document.java                     # Plain POJO for document metadata
└── service/
    ├── BlobStoreService.java             # S3 operations (upload/download/list/delete)
    ├── DocumentMetadataService.java      # DynamoDB CRUD using low-level client
    └── StudentDocumentManager.java       # Orchestrates S3 + DynamoDB together

src/test/java/
└── StudentDocumentManagerTest.java       # Unit tests with Mockito (6 tests)
```

## Tech Stack

- Java 17, Maven
- AWS SDK v2 (`s3`, `dynamodb`) — low-level client, no beans/annotations
- SLF4J + Logback
- JUnit 5 + Mockito
