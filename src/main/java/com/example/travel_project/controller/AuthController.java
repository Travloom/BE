package com.example.travel_project.controller;

import com.example.travel_project.domain.user.web.dto.ProfileDTO;
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
    public ResponseEntity<ProfileDTO> getProfile(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        if (principal == null) {
            System.out.println("principal is null");
            return ResponseEntity.status(401).body(new ProfileDTO(null, null, null));
        }

        // 1. get properties와 kakao_account map
        Map<String, Object> properties = (Map<String, Object>) principal.getAttribute("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) principal.getAttribute("kakao_account");

        String name = null;
        String profileImageUrl = null;
        String email = null;

        if (kakaoAccount != null) {
            // 이름
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                name = (String) profile.get("nickname");
                profileImageUrl = (String) profile.get("profile_image_url");
            }
            email = (String) kakaoAccount.get("email");
        }

        if (name == null && properties != null) {
            name = (String) properties.get("nickname");
        }
        if (profileImageUrl == null && properties != null) {
            profileImageUrl = (String) properties.get("profile_image");
        }

        ProfileDTO dto = new ProfileDTO(name, profileImageUrl, email);
        return ResponseEntity.ok(dto);
    }

    // 기존 logout-url 출력 로직은 그대로 유지
    @GetMapping("/logout-url")
    public ResponseEntity<Map<String, String>> getLogoutUrl() {
        String kakaoLogoutUrl =
                "https://kauth.kakao.com/oauth/logout" +
                        "?client_id=0b7e017adc7d70ae11481fa9ad8777b0" +
                        "&logout_redirect_uri=http://localhost:3000";
        return ResponseEntity.ok(Map.of("logoutUrl", kakaoLogoutUrl));
    }
}
