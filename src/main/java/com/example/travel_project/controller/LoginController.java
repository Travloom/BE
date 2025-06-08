package com.example.travel_project.controller;

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

    private final WebClient webClient = WebClient.create();

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         @AuthenticationPrincipal OAuth2User oauth2User,
                         OAuth2AuthenticationToken authentication) throws ServletException {

        request.logout(); // Spring Security 세션 정리

        if (oauth2User != null && authentication != null) {
            String registrationId = authentication.getAuthorizedClientRegistrationId();
            String name = authentication.getName();
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(registrationId, name);
            if (authorizedClient != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();

                // 카카오 연결 끊기 요청
                webClient.post()
                        .uri("https://kapi.kakao.com/v1/user/unlink")
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(error -> System.out.println("카카오 unlink 실패: " + error.getMessage()))
                        .subscribe();
            }
        }

        return "redirect:/";
    }


    @GetMapping("/welcome")
    public String welcome(Model model,
                          @AuthenticationPrincipal OAuth2User oauth2User,
                          OAuth2AuthenticationToken authentication) {

        String profileImageUrl = null;
        String nickname = "사용자";

        if (oauth2User != null && authentication != null) {
            String registrationId = authentication.getAuthorizedClientRegistrationId(); // "kakao"
            String name = authentication.getName();

            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(registrationId, name);
            if (authorizedClient != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();

                try {
                    // 카카오 계정의 프로필 정보를 요청하는 URL
                    Map<String, Object> response = webClient.get()
                            .uri("https://kapi.kakao.com/v2/user/me")
                            .headers(headers -> headers.setBearerAuth(accessToken)) // 인증 헤더 추가
                            .retrieve()
                            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                    clientResponse -> Mono.error(new RuntimeException("API 호출 실패"))) // 예외 처리
                            .bodyToMono(Map.class) // 응답을 Map 형식으로 받음
                            .block(); // 응답 기다리기

                    // 프로필 사진 URL 가져오기
                    if (response != null) {
                        Map<String, Object> properties = (Map<String, Object>) response.get("properties");
                        if (properties != null) {
                            profileImageUrl = (String) properties.get("profile_image"); // 카카오 계정 프로필 이미지 URL
                        }
                    }
                } catch (WebClientResponseException e) {
                    // 오류 발생 시 로그 출력
                    System.out.println("카카오톡 프로필 가져오기 오류: " + e.getMessage());
                }
            }

            // 닉네임은 기존 properties에서 가져옴
            if (oauth2User.getAttribute("properties") != null) {
                Map<String, Object> properties = (Map<String, Object>) oauth2User.getAttribute("properties");
                nickname = properties.get("nickname").toString();
            }
        }

        model.addAttribute("name", nickname);
        model.addAttribute("profileImageUrl", profileImageUrl);
        return "welcome";
    }
}
