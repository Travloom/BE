package com.example.travel_project.security;

import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.user.repository.UserRepository;
import com.example.travel_project.domain.user.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final boolean isProd;

    @Value("${app.oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public OAuth2LoginSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            Environment env
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.isProd = "prod".equals(env.getProperty("spring.profiles.active", "dev"));
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth2 authentication expected");
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();

        // (1) email
        String email = (String) oauthUser.getAttribute("email");
        if (email == null) {
            Object kakaoAccount = oauthUser.getAttribute("kakao_account");
            if (kakaoAccount instanceof Map<?, ?> accountMap) {
                Object emailObj = accountMap.get("email");
                if (emailObj != null) email = emailObj.toString();
            }
        }

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot extract email from OAuth2 provider");
            return;
        }

        // (2) nickname, profileImageUrl 통일 네이밍
        String nickname = null;
        String profileImageUrl = null;
        Map<String, Object> additionalClaims = new HashMap<>();
        Object props = oauthUser.getAttribute("properties");
        if (props instanceof Map<?, ?> p) {
            if (p.get("nickname") != null) {
                nickname = (String) p.get("nickname");
                additionalClaims.put("name", nickname);
            }
            if (p.get("profile_image") != null) {
                profileImageUrl = (String) p.get("profile_image");
                additionalClaims.put("profileImageUrl", profileImageUrl);
            }
        }

        // (3) 사용자 정보 DB 저장/업데이트
        User user = userRepository.findByEmail(email)
                .orElse(User.builder()
                        .email(email)
                        .name(nickname != null ? nickname : "")
                        .profileImageUrl(profileImageUrl != null ? profileImageUrl : "")
                        .build());
        user.setName(nickname != null ? nickname : "");
        user.setProfileImageUrl(profileImageUrl != null ? profileImageUrl : "");
        userRepository.save(user);

        // (4) accessToken/refreshToken
        String accessToken = jwtTokenProvider.createToken(email, additionalClaims);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        refreshTokenService.createRefreshToken(user, refreshToken);

        // (5) Secure: prod 환경이면 true, dev면 false
        boolean secure = isProd;

        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(secure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(secure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (refreshExpirationMs / 1000));

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        response.sendRedirect(successRedirectUrl);
    }
}
