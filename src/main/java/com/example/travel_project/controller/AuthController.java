package com.example.travel_project.controller;

import com.example.travel_project.dto.ProfileDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 로그인 방식에 상관없이 OAuth2AuthenticatedPrincipal을 사용하여 프로필 반환
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> getProfile(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String name = principal.getAttribute("name");
        String profileImageUrl = principal.getAttribute("profileImageUrl");
        String email = principal.getAttribute("email");

        ProfileDto dto = new ProfileDto(name, profileImageUrl, email);
        return ResponseEntity.ok(dto);
    }

    // 기존 logout-url 출력 로직은 그대로 유지
    @GetMapping("/logout-url")
    public ResponseEntity<Map<String, String>> getLogoutUrl() {
        String kakaoLogoutUrl =
                "https://kauth.kakao.com/oauth/logout" +
                        "?client_id=0b7e017adc7d70ae11481fa9ad8777b0" +
                        "&logout_redirect_uri=http://localhost:8080";
        return ResponseEntity.ok(Map.of("logoutUrl", kakaoLogoutUrl));
    }
}
