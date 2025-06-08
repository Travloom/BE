// src/main/java/com/example/travel_project/config/SecurityConfig.java
package com.example.travel_project.config;

import com.example.travel_project.security.JwtTokenProvider;
import com.example.travel_project.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * (1) CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * (2) Kakao Introspection 용 Introspector 빈
     */
    @Bean
    public OpaqueTokenIntrospector kakaoIntrospector() {
        return token -> {
            // (a) Authorization 헤더에 Bearer 토큰 설정
            org.springframework.http.HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            // (b) RestTemplate으로 카카오 프로필 API 호출
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            ).getBody();

            if (body == null || !body.containsKey("kakao_account")) {
                throw new IllegalStateException("Failed to introspect Kakao token");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");

            @SuppressWarnings("unchecked")
            Map<String, Object> profile = kakaoAccount.containsKey("profile")
                    ? (Map<String, Object>) kakaoAccount.get("profile")
                    : Map.of();

            // (c) 카카오 계정에서 이메일·닉네임 추출
            String email = kakaoAccount.getOrDefault("email", "").toString();
            String name  = profile.getOrDefault("nickname", "").toString();

            // (d) ROLE_USER 권한을 GrantedAuthority 타입으로 선언
            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            // (e) 인증된 Principal 정보로 변환
            Map<String, Object> attributes = Map.of(
                    "email", email,
                    "name", name
            );

            return new DefaultOAuth2AuthenticatedPrincipal(
                    attributes,
                    authorities
            );
        };
    }

    /**
     * (3) 인증 에러 시 401 응답을 내리는 헬퍼 메서드
     */
    private void unauthorizedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        } catch (Exception ignore) {}
    }

    /**
     * (4) JWT 인증 필터 (UsernamePasswordAuthenticationFilter 상속)
     *     - Authorization 헤더에 "Bearer <JWT>"가 있으면 토큰 검증 후 SecurityContext 설정
     */
    public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
        @Override
        protected boolean requiresAuthentication(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response) {

            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            return header != null && header.startsWith("Bearer ");
        }

        @Override
        public org.springframework.security.core.Authentication attemptAuthentication(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response) {

            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith("Bearer ")) {
                return null;
            }

            String token = header.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getSubject(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
                return auth;
            }
            return null;
        }
    }

    /**
     * (5) /api/** 영역 보안 설정
     *     - JWT 인증 필터를 먼저 시도하고, 실패 시 Kakao Introspection 으로 대체 인증
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/logout-url", "/api/auth/profile").permitAll()
                        .anyRequest().authenticated()
                )
                // (a) JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // (b) JWT 인증이 안 되면 Kakao Introspection 으로 토큰 검사
                .oauth2ResourceServer(oauth2 -> oauth2
                        .opaqueToken(token -> token.introspector(kakaoIntrospector()))
                )
                // (c) 둘 다 실패했을 때 401 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, ex2) -> unauthorizedResponse(res))
                );

        return http.build();
    }

    /**
     * (6) 웹(UI) 전용 보안 설정
     *     - OAuth2 로그인(카카오) 성공 후 JWT 발급용 SuccessHandler 등록
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl(successRedirectUrl, true)
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl(successRedirectUrl)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}
