package com.example.pitchmateserver.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * QR-REL-04 고아 작업 정리.
 * 분석은 서버 메모리의 비동기 스레드에서만 진행되므로, 서버가 재시작되면 진행 중이던 작업은 다시 이어지지 않는다.
 * 이런 작업이 "분석 중"으로 영구히 남지 않도록 FAILED로 전환해 사용자가 재요청할 수 있게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleAnalysisCleaner {

    // 이 시간 동안 상태 갱신이 없으면 멈춘 작업으로 본다 (SRS QR-REL-04: 10분)
    private static final Duration STALE_TIMEOUT = Duration.ofMinutes(10);

    private static final String RESTART_REASON = "서버 재시작으로 AI 처리가 중단되었습니다. 분석을 다시 요청해주세요.";
    private static final String STALE_REASON = "AI 처리가 응답 없이 멈춰 중단되었습니다. 분석을 다시 요청해주세요.";

    private final AnalysisService analysisService;

    // 이 인스턴스가 뜨기 전에 갱신된 진행 중 작업은 이전 프로세스의 것이므로 이어질 수 없다
    private final LocalDateTime startedAt = LocalDateTime.now();

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedOnStartup() {
        int count = analysisService.failStaleAnalyses(startedAt, RESTART_REASON);
        if (count > 0) {
            log.warn("서버 재시작으로 중단된 분석 {}건을 FAILED로 전환", count);
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void failStale() {
        int count = analysisService.failStaleAnalyses(LocalDateTime.now().minus(STALE_TIMEOUT), STALE_REASON);
        if (count > 0) {
            log.warn("{}분 넘게 갱신 없는 분석 {}건을 FAILED로 전환", STALE_TIMEOUT.toMinutes(), count);
        }
    }
}
