package com.example.travel_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 퍼블릭 리소스 허용
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()
                        // 보호된 리소스는 인증된 사용자만
                        .requestMatchers("/logout", "/map", "/users").authenticated()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 성공 후 환영 페이지로 리다이렉트
                        .defaultSuccessUrl("/welcome", true)
                );
        return http.build();
    }
}