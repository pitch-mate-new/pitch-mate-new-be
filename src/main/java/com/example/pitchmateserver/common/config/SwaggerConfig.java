package com.example.pitchmateserver.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PitchMate API")
                        .description("""
                                PitchMate 서버 API 명세서입니다.

                                **인증이 필요한 API**는 우측 상단 Authorize 버튼을 클릭하여 \
                                로그인 후 발급받은 accessToken을 입력해주세요.

                                **에러 응답 형식**
                                ```json
                                {
                                  "code": 4001,
                                  "status": 409,
                                  "message": "이미 사용 중인 이메일입니다."
                                }
                                ```

                                **전체 에러 코드**
                                - 4000: 입력값이 올바르지 않습니다
                                - 4001: 이미 사용 중인 이메일입니다
                                - 4002: 이미 사용 중인 닉네임입니다
                                - 4003: 이메일 또는 비밀번호가 올바르지 않습니다
                                - 4004: 유효하지 않은 리프레시 토큰입니다
                                - 4005: 만료된 리프레시 토큰입니다
                                - 4006: 인증이 필요합니다 (토큰 없음/만료 - 401)
                                - 4007: 사용자를 찾을 수 없습니다
                                - 4008: 영상을 찾을 수 없습니다
                                - 4009: 해당 영상에 접근 권한이 없습니다
                                - 4010: 분석 결과를 찾을 수 없습니다
                                - 4011: 이미 분석이 진행 중입니다
                                - 4012: 평가 결과를 찾을 수 없습니다
                                - 4013: 피드백을 찾을 수 없습니다
                                - 4014: 루브릭을 찾을 수 없습니다
                                - 4015: 히스토리를 찾을 수 없습니다
                                - 4016: 파일 업로드에 실패했습니다
                                - 4017: 이미 신청하거나 연결된 멘토입니다
                                - 4018: 연결 정보를 찾을 수 없습니다
                                - 4019: 해당 연결에 접근 권한이 없습니다
                                - 4020: 멘티는 최대 5명의 멘토에게 신청할 수 있습니다
                                - 4021: 멘토는 최대 10명의 멘티와 연결할 수 있습니다
                                - 4022: 멘토 역할의 사용자만 가능합니다
                                - 4023: 연결된 멘토에게만 피드백을 요청할 수 있습니다
                                - 4024: 지원하지 않는 파일 형식입니다 (MP4, MOV, AVI, WEBM만 허용)
                                - 4025: 이미 이 영상에 총평을 작성했습니다
                                - 5000: 서버 오류가 발생했습니다
                                """)
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(jwtScheme))
                .components(new Components()
                        .addSecuritySchemes(jwtScheme, new SecurityScheme()
                                .name(jwtScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 후 발급받은 accessToken을 입력하세요. (Bearer 접두사 없이)")));
    }
}
