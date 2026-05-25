package com.example.pitchmateserver.common.video;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoMetadataService {

    @Value("${ffmpeg.path:/usr/bin/ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:/usr/bin/ffprobe}")
    private String ffprobePath;

    public Integer extractDuration(File videoFile) {
        try {
            FFprobe ffprobe = new FFprobe(ffprobePath);
            FFmpegProbeResult result = ffprobe.probe(videoFile.getAbsolutePath());
            double duration = result.getFormat().duration;
            return (int) Math.round(duration);
        } catch (Exception e) {
            log.warn("영상 길이 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public byte[] extractThumbnail(File videoFile) {
        File thumbnailFile = null;
        try {
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);

            thumbnailFile = File.createTempFile("thumb_", ".jpg");

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(videoFile.getAbsolutePath())
                    .overrideOutputFiles(true)
                    .addOutput(thumbnailFile.getAbsolutePath())
                    .setFrames(1)
                    .setStartOffset(1, TimeUnit.SECONDS)
                    .done();

            new FFmpegExecutor(ffmpeg, ffprobe).createJob(builder).run();
            return Files.readAllBytes(thumbnailFile.toPath());
        } catch (Exception e) {
            log.warn("썸네일 추출 실패: {}", e.getMessage());
            return null;
        } finally {
            if (thumbnailFile != null) thumbnailFile.delete();
        }
    }

    public File toTempFile(byte[] bytes, String extension) throws IOException {
        File temp = File.createTempFile("video_", extension);
        Files.write(temp.toPath(), bytes);
        return temp;
    }
}
