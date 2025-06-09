// src/main/java/com/example/travel_project/security/OAuth2LoginSuccessHandler.java
package com.example.travel_project.security;

import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 로그인(카카오) 성공 시 호출되는 핸들러.
 * 카카오에서 가져온 사용자 이메일 등 정보를 바탕으로 JWT를 생성하여 클라이언트에 반환.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth2 authentication expected");
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();

        // ──────────────────────────────
        // (1) 카카오 Introspection 또는 사용자 정보에서 이메일 꺼내기
        //    kakaoIntrospector가 attributes에 "email"을 담아 주었으므로 꺼내면 됩니다.
        String email = null;
        Object kakaoAccount = oauthUser.getAttribute("kakao_account");
        if (kakaoAccount instanceof Map<?, ?> accountMap) {
            Object emailObj = accountMap.get("email");
            if (emailObj != null) {
                email = emailObj.toString();
            }
        }
        if (email == null) {
            // fallback: attributes에 바로 "email"로 넣어준 경우
            Object fallbackEmail = oauthUser.getAttribute("email");
            if (fallbackEmail != null) {
                email = fallbackEmail.toString();
            }
        }

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot extract email from Kakao account");
            return;
        }

        String nickname = null;
        String profileImageUrl = null;

        // (2) 추가 클레임(ex: 닉네임, 프로필 이미지)을 직접 꺼내서 넣어줄 수 있음
        Map<String, Object> additionalClaims = new HashMap<>();
        Object props = oauthUser.getAttribute("properties");
        if (props instanceof Map<?, ?> p) {
            if (p.get("nickname") != null) {
                nickname = (String) p.get("nickname");
                additionalClaims.put("name", p.get("nickname"));
            }
            if (p.get("profile_image") != null) {
                profileImageUrl = (String) p.get("profile_image");
                additionalClaims.put("picture", p.get("profile_image"));
            }
        }
        // ──────────────────────────────

        User user = User.builder()
                .email(email)
                .name(nickname != null ? nickname : "")
                .profileImageUrl(profileImageUrl != null ? profileImageUrl : "")
                .build();

        if (!userRepository.existsByEmail(email)) {
            userRepository.save(user);
        }

        // (3) JWT 생성
        String jwt = jwtTokenProvider.createToken(email, additionalClaims);

        // 쿠키에 저장
        Cookie cookie = new Cookie("accessToken", jwt);
        cookie.setHttpOnly(true); // JavaScript에서 접근 못 하게 (XSS 방지)
        cookie.setSecure(false); // HTTPS에서만 전송되게
        cookie.setPath("/"); // 전체 경로에서 접근 가능
        cookie.setMaxAge(60 * 60); // 유효 시간 (초) – 예: 1시간

        response.addCookie(cookie);

        // ✅ (2) 클라이언트를 프론트엔드 페이지로 리다이렉트
        response.sendRedirect("http://localhost:3000");
    }
}
