package com.example.travel_project.domain.plan.web.controller;

import com.example.travel_project.domain.plan.web.dto.InviteRequestDTO;
import com.example.travel_project.domain.plan.web.dto.PlanDTO;
import com.example.travel_project.domain.plan.web.dto.PlanRequestDTO;
import com.example.travel_project.domain.plan.data.Plan;
import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.plan.data.mapping.UserPlanList;
import com.example.travel_project.domain.plan.repository.PlanRepository;
import com.example.travel_project.domain.plan.repository.UserPlanListRepository;
import com.example.travel_project.domain.user.repository.UserRepository;
import com.example.travel_project.domain.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanApiController {

    private final PlanRepository planRepository;
    private final PlanService planService;
    private final UserRepository userRepository;
    private final UserPlanListRepository userPlanListRepository;

    /** 전체 플랜 조회 (내가 만든 모든 플랜 목록) */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> listPlans(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime before,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime after,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        String email = principal.getAttribute("email");

        List<Plan> plans = planRepository.findByAuthorEmail(email);

        Stream<Plan> filteredPlans = plans.stream();

        if (before != null) {
            filteredPlans = filteredPlans.filter(p -> p.getStartDate().isBefore(before));
        }

        if (after != null) {
            filteredPlans = filteredPlans.filter(p -> !p.getStartDate().isBefore(after));
        }

        if (year != null && month != null) {
            LocalDateTime startOfPrevMonth = LocalDateTime.of(year, month, 1, 1, 1).minusMonths(1);
            LocalDateTime endOfNextMonth = LocalDateTime.of(year, month, 1, 1, 1).plusMonths(1).withDayOfMonth(1).plusMonths(1).minusDays(1);

            filteredPlans = filteredPlans.filter(p ->
                    (p.getStartDate() != null && !p.getStartDate().isBefore(startOfPrevMonth) && !p.getStartDate().isAfter(endOfNextMonth)) ||
                    (p.getEndDate() != null && !p.getEndDate().isBefore(startOfPrevMonth) && !p.getEndDate().isAfter(endOfNextMonth))
            );
        }

        List<PlanDTO> results = filteredPlans
                .map(p -> new PlanDTO(
                        p.getUuid(),      // uuid로 변경
                        p.getTitle(),
                        p.getStartDate(),
                        p.getEndDate(),
                        p.getContent(),
                        p.getAuthorEmail()
                ))
                .toList();

        return ResponseEntity.ok(results);
    }

    /** 플랜 생성 */
    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(
            @RequestBody PlanRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");

        Plan plan = Plan.builder()
                .title(req.getTitle())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .authorEmail(email)
                .build();

        PlanDTO planDTO = planService.createPlan(plan);

        return ResponseEntity.ok(planDTO);
    }

    /** 단일 플랜 조회 */
    @GetMapping("plan/{uuid}")
    public ResponseEntity<PlanDTO> getPlan(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan UUID: " + uuid));
        if (!email.equals(p.getAuthorEmail())) {
            return ResponseEntity.status(403).build();
        }
        PlanDTO dto = new PlanDTO(
                p.getUuid(),
                p.getTitle(),
                p.getStartDate(),
                p.getEndDate(),
                p.getContent(),
                p.getAuthorEmail()
        );
        return ResponseEntity.ok(dto);
    }

    /** 플랜 수정 */
    @PutMapping("plan/{uuid}")
    public ResponseEntity<PlanDTO> updatePlan(
            @PathVariable String uuid,
            @RequestBody PlanRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan UUID: " + uuid));

        // (A) 플랜 소유자 체크
        boolean isOwner = email.equals(p.getAuthorEmail());

        // (B) UserPlanList에 참여자로 등록된 멤버 체크
        boolean isCollaborator = userPlanListRepository
                .findByPlanId(p.getId())
                .stream()
                .anyMatch(upl -> upl.getUser().getEmail().equals(email));

        // (C) 둘 다 아니면 수정 거부
        if (!isOwner && !isCollaborator) {
            return ResponseEntity.status(403).build();
        }

        // (D) 플랜 내용 수정
        p.setTitle(req.getTitle());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setContent(req.getContent());
        Plan updated = planRepository.save(p);

        PlanDTO dto = new PlanDTO(
                updated.getUuid(),
                updated.getTitle(),
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getContent(),
                updated.getAuthorEmail()
        );
        return ResponseEntity.ok(dto);
    }

    /** 플랜 삭제 */
    @DeleteMapping("plan/{uuid}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan UUID: " + uuid));
        if (!email.equals(p.getAuthorEmail())) {
            return ResponseEntity.status(403).build();
        }
        planRepository.delete(p);
        return ResponseEntity.noContent().build();
    }

    /** 플랜 초대 **/
    @PostMapping("/plan/invite/{planId}")
    public ResponseEntity<String> inviteUser(
            @PathVariable Long planId,
            @RequestBody InviteRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = req.getEmail(); // 초대할 유저 이메일
        String inviterEmail = principal.getAttribute("email");

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("플랜 없음"));
        if (!plan.getAuthorEmail().equals(inviterEmail)) {
            return ResponseEntity.status(403).body("플랜 소유자만 초대 가능");
        }

        User invitee = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대할 유저가 존재하지 않습니다."));

        UserPlanList userPlanList = UserPlanList.builder()
                .user(invitee)
                .plan(plan)
                .build();

        userPlanListRepository.save(userPlanList);

        return ResponseEntity.ok("초대 완료");
    }
}
