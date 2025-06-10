package com.example.travel_project.domain.plan.web.controller;

import com.example.travel_project.domain.plan.web.dto.InviteRequestDTO;
import com.example.travel_project.domain.plan.web.dto.PlanDTO;
import com.example.travel_project.domain.plan.web.dto.PlanRequestDTO;
import com.example.travel_project.domain.plan.data.Plan;
import com.example.travel_project.domain.plan.web.dto.TagDTO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
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

    /** 전체 플랜 조회 **/
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> listPlans(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        String email = principal.getAttribute("email");

        List<Plan> plans = planRepository.findByAuthorEmail(email);

        Stream<Plan> filteredPlans = plans.stream();

        if (before != null) {
            filteredPlans = filteredPlans.filter(p -> p.getEndDate().isBefore(before));
        }

        else if (after != null) {
            filteredPlans = filteredPlans.filter(p -> !p.getEndDate().isBefore(after));
        }

        else if (year != null && month != null) {
            LocalDateTime startOfPrevMonth = LocalDateTime.of(year, month, 1, 1, 1).minusMonths(1);
            LocalDateTime endOfNextMonth = LocalDateTime.of(year, month, 1, 1, 1).plusMonths(1).withDayOfMonth(1).plusMonths(1).minusDays(1);

            filteredPlans = filteredPlans.filter(p ->
                    (p.getStartDate() != null && !p.getStartDate().isBefore(startOfPrevMonth) && !p.getStartDate().isAfter(endOfNextMonth)) ||
                    (p.getEndDate() != null && !p.getEndDate().isBefore(startOfPrevMonth) && !p.getEndDate().isAfter(endOfNextMonth))
            );
        }

        List<PlanDTO> results = filteredPlans.map(p -> PlanDTO.builder()
                        .uuid(p.getUuid())
                        .title(p.getTitle())
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .content(p.getContent())
                        .authorEmail(p.getAuthorEmail())
                        .tags(TagDTO.builder()
                                .region(p.getRegion())
                                .people(p.getPeople())
                                .companions(p.getCompanions())
                                .theme(p.getTheme())
                                .build())
                        .build()
                )
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    /** 플랜 생성 */
    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(
            @RequestBody PlanRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) throws ExecutionException, InterruptedException {
        String email = principal.getAttribute("email");

        Plan plan = Plan.builder()
                .title(req.getTitle())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .authorEmail(email)
                .region(req.getRegion())
                .people(req.getPeople())
                .companions(req.getCompanions())
                .theme(req.getTheme())
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
        PlanDTO dto = PlanDTO.builder()
                .uuid(p.getUuid())
                .title(p.getTitle())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .content(p.getContent())
                .authorEmail(p.getAuthorEmail())
                .tags(TagDTO.builder()
                        .region(p.getRegion())
                        .people(p.getPeople())
                        .companions(p.getCompanions())
                        .theme(p.getTheme())
                        .build())
                .build();

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
        p.setRegion(req.getRegion());
        p.setPeople(req.getPeople());
        p.setCompanions(req.getCompanions());
        p.setTheme(req.getTheme());
        Plan updated = planRepository.save(p);

        PlanDTO dto = PlanDTO.builder()
                .uuid(updated.getUuid())
                .title(updated.getTitle())
                .startDate(updated.getStartDate())
                .endDate(updated.getEndDate())
                .content(updated.getContent())
                .authorEmail(updated.getAuthorEmail())
                .tags(TagDTO.builder()
                        .region(p.getRegion())
                        .people(p.getPeople())
                        .companions(p.getCompanions())
                        .theme(p.getTheme())
                        .build())
                .build();
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

    /** 플랜 나가기 **/
    @DeleteMapping("plan/exit/{uuid}")
    public ResponseEntity<String> exitPlan(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Plan plan = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (plan.getAuthorEmail().equals(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("플랜의 소유자 입니다.");
        }

        UserPlanList userPlanList = userPlanListRepository.findByUserAndPlan(user, plan)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "참여중인 플랜이 아닙니다."));

        userPlanListRepository.delete(userPlanList);

        return ResponseEntity.ok("플랜에서 나갔습니다.");
    }

    /** 플랜 초대 **/
    @PostMapping("/plan/invite/{uuid}")
    public ResponseEntity<String> inviteUser(
            @PathVariable Long uuid,
            @RequestBody InviteRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = req.getEmail(); // 초대할 유저 이메일
        String inviterEmail = principal.getAttribute("email");

        Plan plan = planRepository.findById(uuid)
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
