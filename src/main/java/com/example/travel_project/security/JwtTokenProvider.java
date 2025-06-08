// src/main/java/com/example/travel_project/security/JwtTokenProvider.java
package com.example.travel_project.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증을 담당하는 컴포넌트
 */
@Component
public class JwtTokenProvider {

    /** application.properties에 설정한 Base64 인코딩 비밀키 */
    @Value("${app.jwt.secret}")
    private String secretKey;

    /** application.properties에 설정한 토큰 만료 시간(밀리초) */
    @Value("${app.jwt.expiration-ms}")
    private long validityInMilliseconds;

    private Key key;

    @PostConstruct
    public void init() {
        // Base64로 인코딩된 문자열을 디코딩하여 HMAC-SHA256 키 객체 생성
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT 토큰 생성
     * @param subjectEmail 토큰의 Subject (예: 사용자의 이메일)
     * @param additionalClaims 추가로 넣고 싶은 클레임 (예: name, picture 등). null 가능.
     * @return 발급된 JWT 토큰 문자열
     */
    public String createToken(String subjectEmail, Map<String, Object> additionalClaims) {
        Claims claims = Jwts.claims().setSubject(subjectEmail);
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰에서 Subject(예: 이메일) 추출
     */
    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * JWT 토큰의 유효성 검사 (서명 + 만료시간 체크)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 잘못됐거나, 만료됐거나, 서명 검증 실패 등
            return false;
        }
    }
}
