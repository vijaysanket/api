package com.socialeazy.api.services.impl;



import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
public interface S3Service {
    String uploadFile(String bucketName, String fileName, InputStream fileStream);

    void readFileFromS3Url(String s3Url, BiConsumer<String, String> lineProcessor);

    String uploadFile(MultipartFile mediaFile);
}
