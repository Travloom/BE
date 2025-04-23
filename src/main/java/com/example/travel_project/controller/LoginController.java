package com.example.travel_project.controller;

import com.example.travel_project.model.AppUser;
import com.example.travel_project.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private UserRepository userRepository;

    private final WebClient webClient = WebClient.create();

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        String kakaoLogoutUrl = "https://kauth.kakao.com/oauth/logout?client_id=0b7e017adc7d70ae11481fa9ad8777b0&logout_redirect_uri=http://localhost:8080";
        return "redirect:" + kakaoLogoutUrl;
    }

    @GetMapping("/welcome")
    public String welcome(Model model,
                          @AuthenticationPrincipal OAuth2User oauth2User,
                          OAuth2AuthenticationToken authentication) {

        String profileImageUrl = null;
        String nickname = "사용자";
        String email = null;

        if (oauth2User != null && authentication != null) {
            String registrationId = authentication.getAuthorizedClientRegistrationId(); // "kakao"
            String name = authentication.getName();

            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(registrationId, name);
            if (authorizedClient != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();

                try {
                    // 카카오 계정의 프로필 정보를 요청
                    Map<String, Object> response = webClient.get()
                            .uri("https://kapi.kakao.com/v2/user/me")
                            .headers(headers -> headers.setBearerAuth(accessToken))
                            .retrieve()
                            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                    clientResponse -> Mono.error(new RuntimeException("API 호출 실패")))
                            .bodyToMono(Map.class)
                            .block();

                    if (response != null) {
                        Map<String, Object> properties = (Map<String, Object>) response.get("properties");
                        if (properties != null) {
                            profileImageUrl = (String) properties.get("profile_image");
                            nickname = (String) properties.get("nickname");
                        }

                        Map<String, Object> account = (Map<String, Object>) response.get("kakao_account");
                        if (account != null) {
                            email = (String) account.get("email");

                            // 사용자 정보 DB에 저장 (이미 존재하지 않으면)
                            if (!userRepository.existsByEmail(email)) {
                                AppUser user = new AppUser(nickname, profileImageUrl, email);
                                userRepository.save(user);
                            }
                        }
                    }

                } catch (WebClientResponseException e) {
                    System.out.println("카카오 프로필 가져오기 오류: " + e.getMessage());
                }
            }
        }

        model.addAttribute("name", nickname);
        model.addAttribute("profileImageUrl", profileImageUrl);
        return "welcome";
    }
}
