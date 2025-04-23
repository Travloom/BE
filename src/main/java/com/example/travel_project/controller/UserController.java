package com.example.travel_project.controller;

import com.example.travel_project.model.AppUser;
import com.example.travel_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Secured("ROLE_USER")  // 인증된 사용자만 접근할 수 있게 설정
    @GetMapping("/users")
    public String getUsers(Model model) {
        List<AppUser> users = userRepository.findAll();  // 사용자 목록 가져오기
        model.addAttribute("users", users);
        return "users";  // users.html 페이지로 반환
    }
}
