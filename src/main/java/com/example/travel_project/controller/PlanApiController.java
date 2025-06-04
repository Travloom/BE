package com.example.travel_project.controller;

import com.example.travel_project.dto.PlanDTO;
import com.example.travel_project.dto.PlanRequestDTO;
import com.example.travel_project.entity.Plan;
import com.example.travel_project.repository.PlanRepository;
import com.example.travel_project.repository.UserPlanListRepository;
import com.example.travel_project.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanApiController {

    private final PlanRepository planRepository;
    private final PlanService planService;
    private final UserPlanListRepository userPlanListRepository;

    /** 전체 플랜 조회 (내가 만든 모든 플랜 목록) */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> listPlans(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        List<PlanDTO> plans = planRepository.findByAuthorEmail(email).stream()
                .map(p -> new PlanDTO(
                        p.getUuid(),      // uuid로 변경
                        p.getTitle(),
                        p.getStartDate(),
                        p.getEndDate(),
                        p.getContent(),
                        p.getAuthorEmail()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(plans);
    }

    /** 플랜 생성 */
    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(
            @RequestBody PlanRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        Map<String, Object> kakaoAccound = (Map<String, Object>) principal.getAttribute("kakao_account");

        if (kakaoAccound == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = kakaoAccound.get("email").toString();

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
}
