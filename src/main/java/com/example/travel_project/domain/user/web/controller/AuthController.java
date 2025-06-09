package com.example.travel_project.domain.user.web.controller;

import com.example.travel_project.domain.user.web.dto.ProfileDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logOut(
            HttpServletResponse response) {

        Cookie deleteCookie = new Cookie("accessToken", null);
        deleteCookie.setHttpOnly(true);
        deleteCookie.setSecure(true);
        deleteCookie.setPath("/"); // 생성할 때와 동일한 경로여야 함
        deleteCookie.setMaxAge(0); // 0초로 설정하면 삭제됨
        response.addCookie(deleteCookie);

        return ResponseEntity.ok(Map.of("isLogout", "true"));
    }
}

