package com.socialeazy.api.services.impl;


import com.socialeazy.api.services.impl.S3Service;
import com.socialeazy.api.services.impl.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${spring.ngrokUrl}")
    private String ngrokUrl;

    public S3ServiceImpl(@Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
                         @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey,
                         @Value("${spring.cloud.aws.region.static}") String region,
                         @Value("${spring.cloud.aws.s3.endpoint}") String s3Endpoint) {

        S3ClientBuilder s3Builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true); // Required for LocalStack

        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            s3Builder.endpointOverride(URI.create(s3Endpoint));
        }

        this.s3Client = s3Builder.build();
        log.info("S3 Client initialized successfully");
    }

    private String[] extractBucketAndKey(String s3Url) {
        try {
            URI uri = new URI(s3Url);
            String host = uri.getHost();
            String path = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath(); // Remove leading slash

            // Handle LocalStack (e.g., http://localhost:4566/my-bucket/my-file.txt)
            if (host.startsWith("localhost") || host.startsWith("127.0.0.1")) {
                String[] parts = path.split("/", 2);
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Invalid S3 LocalStack URL: " + s3Url);
                }
                return new String[]{parts[0], parts[1]};
            }

            // Handle AWS Virtual-Hosted Style URLs (e.g., https://my-bucket.s3.us-east-1.amazonaws.com/my-file.txt)
            Pattern pattern = Pattern.compile("^(.*?)\\.s3[.-]([a-z0-9-]+)\\.amazonaws\\.com$");
            Matcher matcher = pattern.matcher(host);
            if (matcher.matches()) {
                return new String[]{matcher.group(1), path};
            }

            // Handle AWS Path-Style URLs (e.g., https://s3.amazonaws.com/my-bucket/my-file.txt)
            if (host.equals("s3.amazonaws.com")) {
                String[] parts = path.split("/", 2);
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Invalid S3 Path-Style URL: " + s3Url);
                }
                return new String[]{parts[0], parts[1]};
            }

            // Handle Custom S3-Compatible Endpoints (e.g., MinIO, DigitalOcean Spaces)
            String[] parts = path.split("/", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid Custom S3 URL: " + s3Url);
            }
            return new String[]{parts[0], parts[1]};

        } catch (Exception e) {
            throw new RuntimeException("Error while extracting bucket and key from S3 URL: " + s3Url, e);
        }
    }


//    @Override
//    public void readFileFromS3Url(String s3Url, BiConsumer<String, String> lineProcessor) {
//        String[] bucketAndKey = extractBucketAndKey(s3Url);
//        if (bucketAndKey == null) {
//            throw new IllegalArgumentException("Invalid S3 URL: " + s3Url);
//        }
//
//        String bucketName = bucketAndKey[0];
//        String key = bucketAndKey[1];
//
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                .bucket(bucketName)
//                .key(key)
//                .build();
//
//        try (InputStream inputStream = s3Client.getObject(getObjectRequest); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//            String header = reader.readLine();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                lineProcessor.accept(header, line);  // Process each line independently
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException("Error reading file from S3", e);
//        }
//    }

    @Override
    public String uploadFile(String bucketName, String fileName, InputStream fileStream) {
        try {
            // Generate unique file name to avoid conflicts
            String uniqueFileName = UUID.randomUUID() + "_" + Paths.get(fileName).getFileName().toString();

            boolean bucketExists = s3Client.listBuckets().buckets().stream()
                    .anyMatch(b -> b.name().equals(bucketName));

            if (!bucketExists) {
                throw new RuntimeException("Bucket does not exist: " + bucketName);
            }

            System.out.println(uniqueFileName);
            // Prepare request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(fileStream, fileStream.available()));

            S3Utilities s3Utilities = s3Client.utilities();
            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();

            String fileUrl = s3Utilities.getUrl(getUrlRequest).toString();

            if (fileUrl.contains("http://localhost:4566") && ngrokUrl != null) {
                fileUrl = fileUrl.replace("http://localhost:4566", ngrokUrl);
            }


            // Return the public URL of the uploaded file
            return fileUrl;

        } catch (Exception e) {
            log.info("Exception while storing file in S3 :: {}", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public void readFileFromS3Url(String s3Url, BiConsumer<String, String> lineProcessor) {

    }

    @Override
    public String uploadFile(MultipartFile mediaFile) {
        return "";
    }
}
