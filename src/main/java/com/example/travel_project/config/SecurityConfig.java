// src/main/java/com/example/travel_project/config/SecurityConfig.java
package com.example.travel_project.config;

import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.user.repository.UserRepository;
import com.example.travel_project.security.JwtTokenProvider;
import com.example.travel_project.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    @Autowired
    private UserRepository userRepository;

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
     * (3) 인증 에러 시 401 응답을 내리는 헬퍼 메서드
     */
    private void unauthorizedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        } catch (Exception ignore) {}
    }


    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            String token = null;

            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getSubject(token);

                User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                UsernamePasswordAuthenticationToken auth = getUsernamePasswordAuthenticationToken(email, user);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            else {
                Cookie deleteCookie = new Cookie("accessToken", null);
                deleteCookie.setHttpOnly(true);
                deleteCookie.setSecure(true);
                deleteCookie.setPath("/"); // 생성할 때와 동일한 경로여야 함
                deleteCookie.setMaxAge(0); // 0초로 설정하면 삭제됨
                response.addCookie(deleteCookie);
            }

            filterChain.doFilter(request, response);
        }

        private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(String email, User user) {
            Map<String, Object> attributes = Map.of("email", email, "name", user.getName(), "profileImageUrl", user.getProfileImageUrl());
            OAuth2User oauth2User = new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "email"
            );

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    oauth2User, null, oauth2User.getAuthorities()
            );
            return auth;
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
                        .requestMatchers("/api/auth/logout-url", "/api/auth/profile", "/api/auth/refresh").permitAll()
                        .anyRequest().authenticated()
                )
                // (a) JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // (b) 실패했을 때 401 처리
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
                );

        return http.build();
    }
}
