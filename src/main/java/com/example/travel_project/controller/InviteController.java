package com.example.travel_project.controller;

import com.example.travel_project.entity.Plan;
import com.example.travel_project.entity.User;
import com.example.travel_project.entity.UserPlanList;
import com.example.travel_project.repository.PlanRepository;
import com.example.travel_project.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.travel_project.repository.UserPlanListRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class InviteController {
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserPlanListRepository userPlanListRepository;

    @Operation(
            summary = "유저 초대하기", // Swagger UI에서 API 엔드포인트 옆에 나오는 한줄 설명
            description = ""
    )
    @PostMapping("/{uuid}/invite")
    public ResponseEntity<String> inviteUser(
            @PathVariable String uuid,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = body.get("email"); // 초대할 유저 이메일
        String inviterEmail = principal.getAttribute("email");

        Plan plan = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("플랜 없음"));
        if (!plan.getAuthorEmail().equals(inviterEmail)) {
            return ResponseEntity.status(403).body("플랜 소유자만 초대 가능");
        }

        User invitee = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("초대할 유저가 존재하지 않습니다."));

        // 이미 참여 중인지 체크
        boolean already = userPlanListRepository
                .findByPlanId(plan.getId())
                .stream()
                .anyMatch(upl -> upl.getUser().getEmail().equals(email));
        if (already) {
            return ResponseEntity.badRequest().body("이미 참여중인 유저입니다.");
        }

        // UserPlanList에 바로 추가
        UserPlanList userPlan = new UserPlanList();
        userPlan.setUser(invitee);
        userPlan.setPlan(plan);
        userPlanListRepository.save(userPlan);

        return ResponseEntity.ok("참여 완료");
    }

    @Operation(
            summary = "플랜 나가기",
            description = ""
    )
    @DeleteMapping("/{uuid}/leave")
    public ResponseEntity<String> leavePlan(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        Plan plan = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("플랜 없음"));

        List<UserPlanList> upls = userPlanListRepository.findByPlanId(plan.getId());
        UserPlanList my = upls.stream()
                .filter(upl -> upl.getUser().getId().equals(user.getId()))
                .findFirst().orElse(null);

        if (my == null) {
            return ResponseEntity.badRequest().body("참여 중인 플랜이 아닙니다.");
        }
        userPlanListRepository.delete(my);

        return ResponseEntity.ok("플랜에서 나가기 완료");
    }

}

