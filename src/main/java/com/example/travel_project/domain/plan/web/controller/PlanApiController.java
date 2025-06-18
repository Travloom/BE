package com.example.travel_project.domain.plan.web.controller;

import com.example.travel_project.domain.firestore.service.FirestoreService;
import com.example.travel_project.domain.plan.web.dto.*;
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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
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
    private final FirestoreService firestoreService;

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

        List<PlanDTO> results = planService.getPlans(email, before, after, year, month);

        return ResponseEntity.ok(results);
    }

    /** 플랜 생성 */
    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(
            @RequestBody PlanRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) throws ExecutionException, InterruptedException {
        String email = principal.getAttribute("email");

        System.out.println("출발 : " + req.getStartDate());
        System.out.println("도착 : " + req.getEndDate());

        Plan plan = Plan.builder()
                .title(req.getTitle())
                .startDate(req.getStartDate().toLocalDateTime())
                .endDate(req.getEndDate().toLocalDateTime())
                .authorEmail(email)
                .region(req.getRegion())
                .people(req.getPeople())
                .companions(req.getCompanions())
                .theme(req.getTheme())
                .build();

        System.out.println("출발 : " + plan.getStartDate());

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
        if (!Objects.requireNonNull(email).equals(p.getAuthorEmail())) {
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

        // UserPlanList에 참여자로 등록된 멤버 체크
        boolean isCollaborator = userPlanListRepository
                .findByPlanId(p.getId())
                .stream()
                .anyMatch(upl -> upl.getUser().getEmail().equals(email));

        // 아니면 수정 거부
        if (!isCollaborator) {
            return ResponseEntity.status(403).build();
        }

        // 플랜 내용 수정
        p.setTitle(req.getTitle());
        p.setStartDate(req.getStartDate().toLocalDateTime());
        p.setEndDate(req.getEndDate().toLocalDateTime());
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
    ) throws ExecutionException, InterruptedException {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan UUID: " + uuid));
        if (!Objects.requireNonNull(email).equals(p.getAuthorEmail())) {
            return ResponseEntity.status(403).build();
        }
        planRepository.delete(p);
        firestoreService.deletePlanData(uuid);
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

    /** 유저 플랜 확인 **/
    @GetMapping("plan/is-collaborator/{uuid}")
    public ResponseEntity<IsCollaboratorResponseDTO> isCollaborator(
            @PathVariable String uuid,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        return ResponseEntity.ok(IsCollaboratorResponseDTO.builder()
                    .isCollaborator(planService.isCollaborator(uuid, email))
                    .isExist(planRepository.existsByUuid(uuid))
                    .build());
    }

    /** 플랜 초대 **/
    @PostMapping("/plan/invite/{uuid}")
    public ResponseEntity<InvitePlanResponseDTO> inviteUser(
            @PathVariable String uuid,
            @RequestBody InviteRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = req.getEmail(); // 초대할 유저 이메일
        String inviterEmail = principal.getAttribute("email");

        User invitee = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));
        User inviter = userRepository.findByEmail(inviterEmail).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다."));

        Plan plan = planRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("플랜이 존재하지 않습니다."));

        if (userPlanListRepository.findByUserAndPlan(inviter, plan).isEmpty()) {
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "플랜 참여자만 초대가 가능합니다.");
        }

        planService.joinPlan(uuid, email);

        return ResponseEntity.ok(InvitePlanResponseDTO.builder()
                        .planTitle(plan.getTitle())
                        .userName(invitee.getName())
                .build());
    }
}
