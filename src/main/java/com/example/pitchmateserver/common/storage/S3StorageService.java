package com.example.pitchmateserver.common.storage;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
public class S3StorageService {

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    public String uploadFile(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String key = UUID.randomUUID() + extension;
        String contentType = file.getContentType() != null ? file.getContentType() : "video/mp4";

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            log.info("S3 업로드 완료: {}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public String uploadBytes(byte[] bytes, String extension, String contentType) {
        String key = UUID.randomUUID() + extension;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
            String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            log.info("S3 업로드 완료: {}", publicUrl);
            return publicUrl;
        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public byte[] downloadFile(String publicUrl) {
        try {
            String key = publicUrl.substring(publicUrl.lastIndexOf('/') + 1);

            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            return response.asByteArray();
        } catch (Exception e) {
            log.error("S3 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("영상 파일 다운로드 실패: " + e.getMessage(), e);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return ".mp4";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
