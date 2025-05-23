package com.example.travel_project.controller;

import com.example.travel_project.entity.Plan;
import com.example.travel_project.service.LoginService;
import com.example.travel_project.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final LoginService loginService;

    // 1) 신규 계획 작성 폼
    @GetMapping("/plans/new")
    public String showCreateForm(
            Model model,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        model.addAttribute("plan", new Plan());
        // 로그인한 사용자 이메일 가져오기
        Map<String, String> userInfo = loginService.loadUserProfile(oauth2User, authentication);
        model.addAttribute("email", userInfo.get("email"));
        return "newPlan";
    }

    // 2) 폼에서 전송된 Plan 저장
    @PostMapping("/plans")
    public String createPlan(
            @ModelAttribute Plan plan,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        // 작성자 이메일 설정
        Map<String, String> userInfo = loginService.loadUserProfile(oauth2User, authentication);
        plan.setAuthorEmail(userInfo.get("email"));
        planRepository.save(plan);
        return "redirect:/plans";
    }

    // 3) 저장된 Plan 목록 조회
    @GetMapping("/plans")
    public String listPlans(
            Model model,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        // 1) 로그인 사용자 이메일 조회
        Map<String, String> userInfo = loginService.loadUserProfile(oauth2User, authentication);
        String email = userInfo.get("email");

        // 2) 해당 이메일로 작성된 플랜만 조회
        List<Plan> myPlans = planRepository.findByAuthorEmail(email);
        model.addAttribute("plans", myPlans);

        return "plans";
    }

    @GetMapping("/mypage/plans")
    public String listMyPlans(
            Model model,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken authentication
    ) {
        // 로그인 사용자 이메일 가져오기
        Map<String, String> userInfo = loginService.loadUserProfile(oauth2User, authentication);
        String email = userInfo.get("email");

        // 작성자 본인 이메일로 플랜 조회
        List<Plan> myPlans = planRepository.findByAuthorEmail(email);
        model.addAttribute("plans", myPlans);
        return "plans";  // plans.html 재사용
    }

    /** 1) 편집 폼 보여주기 */
    @GetMapping("/plans/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken auth
    ) {
        // 1. 현재 사용자 이메일
        String email = loginService.loadUserProfile(oauth2User, auth).get("email");
        // 2. Plan 가져오기 & 작성자 체크
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan Id:" + id));
        if (!email.equals(plan.getAuthorEmail())) {
            return "redirect:/plans";  // 본인 것이 아니면 목록으로
        }
        model.addAttribute("plan", plan);
        return "editPlan";
    }

    /** 2) 수정된 Plan 저장하기 */
    @PostMapping("/plans/{id}/edit")
    public String updatePlan(
            @PathVariable Long id,
            @ModelAttribute Plan formPlan,
            @AuthenticationPrincipal OAuth2User oauth2User,
            OAuth2AuthenticationToken auth
    ) {
        String email = loginService.loadUserProfile(oauth2User, auth).get("email");
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan Id:" + id));
        if (!email.equals(plan.getAuthorEmail())) {
            return "redirect:/plans";
        }
        // 폼에서 받은 필드만 업데이트
        plan.setTitle(formPlan.getTitle());
        plan.setStartDate(formPlan.getStartDate());
        plan.setEndDate(formPlan.getEndDate());
        plan.setContent(formPlan.getContent());
        planRepository.save(plan);
        return "redirect:/plans";
    }
}