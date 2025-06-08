package com.example.travel_project.domain.user.web.controller;

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

    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getProfile(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new ProfileDTO(null, null, null));
        }

        String name            = principal.getAttribute("name");
        String profileImageUrl = principal.getAttribute("profileImageUrl");
        String email           = principal.getAttribute("email");

        ProfileDTO dto = new ProfileDTO(name, profileImageUrl, email);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/logout-url")
    public ResponseEntity<Map<String, String>> getLogoutUrl() {
        String kakaoLogoutUrl =
                "https://kauth.kakao.com/oauth/logout" +
                        "?client_id=0b7e017adc7d70ae11481fa9ad8777b0" +
                        "&logout_redirect_uri=http://localhost:3000";
        return ResponseEntity.ok(Map.of("logoutUrl", kakaoLogoutUrl));
    }
}

