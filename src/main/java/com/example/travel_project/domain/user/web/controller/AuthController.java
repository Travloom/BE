package com.example.travel_project.domain.user.web.controller;

import com.example.travel_project.domain.user.data.RefreshToken;
import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.user.repository.UserRepository;
import com.example.travel_project.domain.user.repository.RefreshTokenRepository;
import com.example.travel_project.domain.user.web.dto.ProfileDTO;
import com.example.travel_project.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getProfile(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
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
    public ResponseEntity<Map<String, String>> logOut(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", null);
        Cookie refreshCookie = new Cookie("refreshToken", null);

        boolean secure = "prod".equals(activeProfile);

        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(secure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(secure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("isLogout", "true"));
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token missing");
        }

        // DB에서 리프레시 토큰 확인
        var tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiryDate().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
        }
        RefreshToken token = tokenOpt.get();
        User user = token.getUser();

        // accessToken 재발급
        String accessToken = jwtTokenProvider.createToken(user.getEmail(), Map.of(
                "name", user.getName(),
                "profileImageUrl", user.getProfileImageUrl()
        ));

        // 새 accessToken을 쿠키로 반환 (프론트에서 사용)
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("accessToken", accessToken));
    }
}
