package com.example.travel_project.controller;

import com.example.travel_project.entity.User;
import com.example.travel_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users")
    public String getUsers(Model model) {
        List<User> users = userRepository.findAll();  // 사용자 목록 가져오기
        model.addAttribute("users", users);
        return "users";  // users.html 페이지로 반환
    }
}