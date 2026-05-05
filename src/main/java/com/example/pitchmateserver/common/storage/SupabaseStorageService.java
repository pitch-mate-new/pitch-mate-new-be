package com.example.pitchmateserver.common.storage;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket:videos}")
    private String bucket;

    private final RestClient restClient;

    public SupabaseStorageService() {
        this.restClient = RestClient.create();
    }

    /**
     * Supabase Storage에 파일을 업로드하고 공개 URL을 반환합니다.
     */
    public String uploadFile(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + extension;
        String contentType = file.getContentType() != null ? file.getContentType() : "video/mp4";

        try {
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
            log.info("Supabase Storage 업로드 완료: {}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("Supabase Storage 업로드 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Supabase Storage URL에서 파일 바이트를 다운로드합니다. (Gemini 업로드용)
     */
    public byte[] downloadFile(String publicUrl) {
        try {
            return restClient.get()
                    .uri(publicUrl)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("Supabase Storage 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("영상 파일 다운로드 실패: " + e.getMessage(), e);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return ".mp4";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
