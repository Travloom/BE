package com.example.travel_project.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public OpaqueTokenIntrospector kakaoIntrospector(RestTemplate restTemplate) {
        return token -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) throw new IllegalStateException("Failed to retrieve user info from Kakao");

            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

            String name = profile != null ? (String) profile.get("nickname") : null;
            String picture = profile != null ? (String) profile.get("profile_image_url") : null;
            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("name", name);
            attributes.put("profileImageUrl", picture);
            attributes.put("email", email);

            return new DefaultOAuth2AuthenticatedPrincipal(attributes, Collections.singleton(() -> "ROLE_USER"));
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, OpaqueTokenIntrospector kakaoIntrospector) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/logout-url").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(o -> o
                        .opaqueToken(token -> token.introspector(kakaoIntrospector))
                );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/logout", "/map", "/users").authenticated()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                );
        return http.build();
    }
}

