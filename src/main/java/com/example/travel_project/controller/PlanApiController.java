package com.example.travel_project.controller;

import com.example.travel_project.dto.PlanDto;
import com.example.travel_project.dto.PlanRequestDto;
import com.example.travel_project.entity.Plan;
import com.example.travel_project.entity.User;
import com.example.travel_project.entity.UserPlanList;
import com.example.travel_project.repository.PlanRepository;
import com.example.travel_project.repository.UserPlanListRepository;
import com.example.travel_project.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanApiController {

    private final PlanRepository planRepository;
    private final UserPlanListRepository userPlanListRepository;
    private final UserRepository userRepository;


    /** 전체 플랜 조회 (내가 만든 모든 플랜 목록) */
    @Operation(
            summary = "내가 만든 플랜 목록들 조회",
            description = ""
    )
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDto>> listPlans(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        List<PlanDto> plans = planRepository.findByAuthorEmail(email).stream()
                .map(p -> new PlanDto(
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
    @Operation(
            summary = "새로운 플랜 생성하기",
            description = ""
    )
    @PostMapping("/plan")
    public ResponseEntity<PlanDto> createPlan(
            @RequestBody PlanRequestDto req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = new Plan();
        p.setTitle(req.getTitle());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setContent(req.getContent());
        p.setAuthorEmail(email);
        Plan saved = planRepository.save(p);

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        UserPlanList upl = new UserPlanList();
        upl.setUser(owner);
        upl.setPlan(saved);
        userPlanListRepository.save(upl);

        PlanDto dto = new PlanDto(
                saved.getUuid(),
                saved.getTitle(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getContent(),
                saved.getAuthorEmail()
        );
        URI location = URI.create("/api/plan/" + saved.getUuid());
        return ResponseEntity.created(location).body(dto);
    }

    /** 단일 플랜 조회 */
    @Operation(
            summary = "uuid로 하나의 플랜 조회",
            description = ""
    )
    @GetMapping("/plan/{uuid}")
    public ResponseEntity<PlanDto> getPlan(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan UUID: " + uuid));

        // 1. 플랜 소유자인가?
        boolean isOwner = email.equals(p.getAuthorEmail());

        // 2. 참여자인가? (UserPlanList에 등록되어 있는가?)
        boolean isCollaborator = userPlanListRepository
                .findByPlanId(p.getId())
                .stream()
                .anyMatch(upl -> upl.getUser().getEmail().equals(email));

        if (!isOwner && !isCollaborator) {
            return ResponseEntity.status(403).body(null);
        }

        PlanDto dto = new PlanDto(
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
    public ResponseEntity<PlanDto> updatePlan(
            @PathVariable String uuid,
            @RequestBody PlanRequestDto req,
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

        PlanDto dto = new PlanDto(
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

    //참여자 목록 조회
    @GetMapping("/plan/{uuid}/members")
    public ResponseEntity<List<String>> getMembers(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan plan = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("플랜 없음"));

        // 권한 체크 (참여자 or 소유자)
        boolean isMember = userPlanListRepository.findByPlanId(plan.getId())
                .stream().anyMatch(upl -> upl.getUser().getEmail().equals(email));
        boolean isOwner = plan.getAuthorEmail().equals(email);

        if (!isMember && !isOwner) {
            return ResponseEntity.status(403).build();  // 권한 없음
        }

        // 멤버 목록 추출
        List<String> members = userPlanListRepository.findByPlanId(plan.getId())
                .stream().map(upl -> upl.getUser().getEmail()).toList();

        return ResponseEntity.ok(members);
    }

}
