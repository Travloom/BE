package com.example.travel_project.controller;

import com.example.travel_project.service.LoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class LoginController {

    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        String kakaoLogoutUrl =
                "https://kauth.kakao.com/oauth/logout" +
                        "?client_id=0b7e017adc7d70ae11481fa9ad8777b0" +
                        "&logout_redirect_uri=http://localhost:8080";
        return "redirect:" + kakaoLogoutUrl;
    }

    @GetMapping("/welcome")
    public String welcome(
            Model model,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        // 사용자 프로필 정보
        Map<String, String> userInfo = loginService.loadUserProfile(oauth2User, authentication);
        model.addAttribute("name", userInfo.get("name"));
        model.addAttribute("profileImageUrl", userInfo.get("profileImageUrl"));
        model.addAttribute("email", userInfo.get("email"));

        // 검색 폼 기본값 세팅
        model.addAttribute("region", "");
        model.addAttribute("itinerary", "");
        model.addAttribute("budget", "");
        model.addAttribute("people", "");
        model.addAttribute("companions", "");
        model.addAttribute("theme", "");

        return "welcome";
    }

    @GetMapping("/mypage")
    public String mypage(
            Model model,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        // 서비스 레이어에서 프로필 정보 가져오기
        Map<String, String> userInfo = loginService.loadUserProfile(oauth2User, authentication);

        model.addAttribute("name", userInfo.get("name"));
        model.addAttribute("profileImageUrl", userInfo.get("profileImageUrl"));
        model.addAttribute("email", userInfo.get("email"));

        return "mypage";
    }
}