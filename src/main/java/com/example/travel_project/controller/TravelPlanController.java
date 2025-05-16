package com.example.travel_project.controller;

import com.example.travel_project.entity.TravelPlan;
import com.example.travel_project.service.TravelPlanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/plans")
public class TravelPlanController {

    private final TravelPlanService planService;

    public TravelPlanController(TravelPlanService planService) {
        this.planService = planService;
    }

    // 1) 플랜 목록 보기
    @GetMapping
    public String listPlans(
            @AuthenticationPrincipal OAuth2User oauth2User,
            Model model) {
        List<TravelPlan> plans = planService.getPlans(oauth2User);
        model.addAttribute("plans", plans);
        return "plan/list";    // src/main/resources/templates/plan/list.html
    }

    // 2) 새 플랜 작성 폼
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("plan", new TravelPlan());
        return "plan/form";    // src/main/resources/templates/plan/form.html
    }

    // 3) 플랜 생성 처리
    @PostMapping
    public String createPlan(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @ModelAttribute TravelPlan plan) {
        planService.createPlan(oauth2User, plan);
        return "redirect:/plans";
    }

    // 4) 플랜 상세 보기
    @GetMapping("/{id}")
    public String viewPlan(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User oauth2User,
            Model model) {
        TravelPlan plan = planService.getPlan(id, oauth2User);
        model.addAttribute("plan", plan);
        return "plan/detail";  // src/main/resources/templates/plan/detail.html
    }

    // (필요시) 수정/삭제 메서드 추가...
}
