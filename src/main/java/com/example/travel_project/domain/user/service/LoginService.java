package com.example.travel_project.domain.user.service;

import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class LoginService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;
    private final WebClient webClient = WebClient.create();

    @Autowired
    public LoginService(OAuth2AuthorizedClientService authorizedClientService,
                        UserRepository userRepository) {
        this.authorizedClientService = authorizedClientService;
        this.userRepository = userRepository;
    }

    /**
     * OAuth2User와 인증 토큰을 받아 카카오 프로필 조회, DB 저장 로직 수행
     */
    public Map<String, String> loadUserProfile(
            OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        String profileImageUrl = null;
        String nickname = "사용자";
        String email = null;

        if (oauth2User != null && authentication != null) {
            String registrationId = authentication.getAuthorizedClientRegistrationId();
            String principalName = authentication.getName();

            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient(registrationId, principalName);

            if (authorizedClient != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();

                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = webClient.get()
                            .uri("https://kapi.kakao.com/v2/user/me")
                            .headers(h -> h.setBearerAuth(accessToken))
                            .retrieve()
                            .onStatus(
                                    status -> status.is4xxClientError() || status.is5xxServerError(),
                                    clientResponse -> Mono.error(new RuntimeException("API 호출 실패"))
                            )
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
                            if (email != null && !userRepository.existsByEmail(email)) {
                                User user = new User(nickname, profileImageUrl, email);
                                userRepository.save(user);
                            }
                        }
                    }

                } catch (WebClientResponseException e) {
                    System.err.println("카카오 프로필 가져오기 오류: " + e.getMessage());
                }
            }

            Object props = oauth2User.getAttribute("properties");
            if (props instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) props;
                if (map.get("nickname") != null) {
                    nickname = map.get("nickname").toString();
                }
            }
        }

        Map<String, String> result = new HashMap<>();
        result.put("name", nickname);
        result.put("profileImageUrl", profileImageUrl);
        result.put("email", email);
        return result;
    }
}