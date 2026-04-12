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

                                **주요 에러 코드**
                                - 4001: 이메일 중복
                                - 4002: 닉네임 중복
                                - 4003: 이메일/비밀번호 불일치
                                - 4004: 유효하지 않은 토큰
                                - 4005: 만료된 리프레시 토큰
                                - 4010: 사용자를 찾을 수 없음
                                - 4020: 영상을 찾을 수 없음
                                - 4030: 피드백을 찾을 수 없음
                                - 4040: 평가를 찾을 수 없음
                                - 4050: 분석 결과를 찾을 수 없음
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
