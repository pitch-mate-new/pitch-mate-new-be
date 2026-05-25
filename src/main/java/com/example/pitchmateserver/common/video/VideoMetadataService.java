package com.example.pitchmateserver.common.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Service
public class VideoMetadataService {

    @Value("${ffmpeg.path:/usr/bin/ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:/usr/bin/ffprobe}")
    private String ffprobePath;

    public Integer extractDuration(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "quiet",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();
            if (!output.isEmpty() && !output.equals("N/A")) {
                return (int) Math.round(Double.parseDouble(output));
            }
        } catch (Exception e) {
            log.warn("영상 길이 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    public byte[] extractThumbnail(File videoFile) {
        File thumbnailFile = null;
        try {
            thumbnailFile = File.createTempFile("thumb_", ".jpg");
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-ss", "00:00:01",
                    "-i", videoFile.getAbsolutePath(),
                    "-frames:v", "1",
                    "-q:v", "2",
                    "-y",
                    thumbnailFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getInputStream().readAllBytes(); // consume output
            process.waitFor();
            if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                return Files.readAllBytes(thumbnailFile.toPath());
            }
        } catch (Exception e) {
            log.warn("썸네일 추출 실패: {}", e.getMessage());
        } finally {
            if (thumbnailFile != null) thumbnailFile.delete();
        }
        return null;
    }

    public File toTempFile(byte[] bytes, String extension) throws IOException {
        File temp = File.createTempFile("video_", extension);
        Files.write(temp.toPath(), bytes);
        return temp;
    }
}
