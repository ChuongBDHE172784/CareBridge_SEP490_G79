package com.carebridge.backend.file.service.impl;

import com.carebridge.backend.file.service.IStorageService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "carebridge.storage.provider", havingValue = "r2")
public class R2StorageService implements IStorageService {

    private final S3Client r2S3Client;
    private final S3Presigner r2S3Presigner;

    @Value("${carebridge.storage.r2.bucket}")
    private String bucket;

    @Override
    public void store(String key, byte[] data, String mimeType) {
        try {
            r2S3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(mimeType)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build(), RequestBody.fromBytes(data));
        } catch (S3Exception e) {
            throw new CloudinaryStorageService.StorageException("R2 upload failed", e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, int ttlMinutes) {
        int boundedTtl = Math.max(1, Math.min(ttlMinutes, 15));
        return r2S3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(boundedTtl))
                        .getObjectRequest(request -> request.bucket(bucket).key(key))
                        .build())
                .url().toExternalForm();
    }

    @Override
    public void delete(String key) {
        try {
            r2S3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new CloudinaryStorageService.StorageException("R2 delete failed", e);
        }
    }
}
