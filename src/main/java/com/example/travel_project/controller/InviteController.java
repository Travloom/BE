package com.example.travel_project.controller;

import com.example.travel_project.entity.InviteList;
import com.example.travel_project.entity.Plan;
import com.example.travel_project.entity.User;
import com.example.travel_project.repository.InviteListRepository;
import com.example.travel_project.repository.PlanRepository;
import com.example.travel_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class InviteController {
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final InviteListRepository inviteListRepository;

    @PostMapping("/{planId}/invite")
    public ResponseEntity<String> inviteUser(
            @PathVariable Long planId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = body.get("email"); // 초대할 유저 이메일
        String inviterEmail = principal.getAttribute("email");

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("플랜 없음"));
        if (!plan.getAuthorEmail().equals(inviterEmail)) {
            return ResponseEntity.status(403).body("플랜 소유자만 초대 가능");
        }

        User invitee = userRepository.findAll().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        if (invitee == null) {
            return ResponseEntity.badRequest().body("초대할 유저가 존재하지 않습니다.");
        }

        InviteList invite = new InviteList();
        invite.setUser(invitee);
        invite.setPlan(plan);
        invite.setIsAccepted(false);
        inviteListRepository.save(invite);

        return ResponseEntity.ok("초대 완료");
    }
}

