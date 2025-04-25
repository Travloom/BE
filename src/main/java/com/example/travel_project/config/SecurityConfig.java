package com.example.travel_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 퍼블릭 리소스
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()
                        // 로그아웃, 지도, 사용자 목록은 인증된 사용자만
                        .requestMatchers("/logout").authenticated()
                        .requestMatchers("/map").authenticated()
                        .requestMatchers("/users").authenticated()
                        // 그 외 모든 요청도 인증 필요
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        // 로그아웃 후 루트(/)로 이동
                        .logoutSuccessUrl("/")
                        // 세션 무효화, JSESSIONID 쿠키 삭제
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 성공 시 /welcome으로 강제 리다이렉트
                        .defaultSuccessUrl("/welcome", true)
                );
        return http.build();
    }
}
