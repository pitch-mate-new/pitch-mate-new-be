package com.example.pitchmateserver.connection.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.connection.dto.ConnectionRequest;
import com.example.pitchmateserver.connection.dto.ConnectionResponse;
import com.example.pitchmateserver.connection.dto.MentorSearchResponse;
import com.example.pitchmateserver.connection.service.ConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "멘토 연결", description = "멘토-멘티 연결 신청/수락/거절/조회 API")
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @Operation(
            summary = "멘토 검색",
            description = """
                    닉네임으로 멘토를 검색합니다. 현재 로그인한 멘티와의 연결 상태도 함께 반환합니다.

                    **connectionStatus 값**
                    - `null`: 연결 없음
                    - `PENDING`: 신청 중
                    - `ACCEPTED`: 연결됨
                    - `REJECTED`: 거절됨

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping("/mentors/search")
    public ResponseEntity<ApiResponse<List<MentorSearchResponse>>> searchMentors(
            @CurrentUser Long userId,
            @RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.ok(connectionService.searchMentors(userId, nickname)));
    }

    @Operation(
            summary = "멘토 연결 신청",
            description = """
                    멘티가 멘토에게 연결을 신청합니다.

                    **제약 조건**
                    - 멘티는 최대 5명의 멘토에게 신청(PENDING/ACCEPTED) 가능
                    - 멘토는 최대 10명의 멘티와 연결(ACCEPTED) 가능
                    - 이미 신청했거나 연결된 경우 재신청 불가

                    **에러 응답**
                    - 400 (code 4022): 대상이 멘토가 아님
                    - 400 (code 4020): 멘티 신청 한도 초과(5명)
                    - 400 (code 4021): 멘토 수락 한도 초과(10명)
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4007): 멘토 사용자를 찾을 수 없음
                    - 409 (code 4017): 이미 신청하거나 연결된 멘토
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ConnectionResponse>> requestConnection(
            @CurrentUser Long userId,
            @Valid @RequestBody ConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED,
                        connectionService.requestConnection(userId, request)));
    }

    @Operation(
            summary = "내 연결 목록 조회",
            description = """
                    로그인한 사용자의 연결 목록을 최신순으로 반환합니다.
                    멘토는 멘티 목록, 멘티는 멘토 목록을 반환합니다.
                    응답의 `userId`, `nickname`, `intro`, `profileImage`는 상대방(멘토 또는 멘티) 정보입니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> getMyConnections(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(connectionService.getMyConnections(userId)));
    }

    @Operation(
            summary = "연결 수락 (멘토)",
            description = """
                    멘토가 멘티의 연결 신청을 수락합니다. PENDING 상태인 신청만 수락 가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4019): 본인 연결이 아니거나 이미 처리된 신청
                    - 404 (code 4018): 연결 정보를 찾을 수 없음
                    """
    )
    @PatchMapping("/{connectionId}/accept")
    public ResponseEntity<ApiResponse<ConnectionResponse>> acceptConnection(
            @CurrentUser Long userId,
            @PathVariable Long connectionId) {
        return ResponseEntity.ok(ApiResponse.ok(connectionService.acceptConnection(userId, connectionId)));
    }

    @Operation(
            summary = "연결 거절 (멘토)",
            description = """
                    멘토가 멘티의 연결 신청을 거절합니다. PENDING 상태인 신청만 거절 가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4019): 본인 연결이 아니거나 이미 처리된 신청
                    - 404 (code 4018): 연결 정보를 찾을 수 없음
                    """
    )
    @PatchMapping("/{connectionId}/reject")
    public ResponseEntity<ApiResponse<ConnectionResponse>> rejectConnection(
            @CurrentUser Long userId,
            @PathVariable Long connectionId) {
        return ResponseEntity.ok(ApiResponse.ok(connectionService.rejectConnection(userId, connectionId)));
    }

    @Operation(
            summary = "연결된 멘토 목록 조회 (멘티용)",
            description = """
                    멘티가 ACCEPTED 상태인 멘토 목록을 조회합니다.
                    영상 업로드 시 피드백 요청 멘토 선택에 사용합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping("/mentors/accepted")
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> getAcceptedMentors(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(connectionService.getAcceptedMentors(userId)));
    }

    @Operation(
            summary = "연결 삭제",
            description = """
                    연결을 삭제합니다. 멘토와 멘티 모두 삭제 가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4019): 본인 연결이 아님
                    - 404 (code 4018): 연결 정보를 찾을 수 없음
                    """
    )
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteConnection(
            @CurrentUser Long userId,
            @PathVariable Long connectionId) {
        connectionService.deleteConnection(userId, connectionId);
        return ResponseEntity.ok(ApiResponse.ok(null, "연결이 삭제되었습니다."));
    }
}
