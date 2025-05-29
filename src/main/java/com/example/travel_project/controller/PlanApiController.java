package com.example.travel_project.controller;

import com.example.travel_project.dto.PlanDto;
import com.example.travel_project.dto.PlanRequestDto;
import com.example.travel_project.entity.Plan;
import com.example.travel_project.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanApiController {

    private final PlanRepository planRepository;

    /** 전체 플랜 조회 */
    @GetMapping
    public ResponseEntity<List<PlanDto>> listPlans(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        List<PlanDto> plans = planRepository.findByAuthorEmail(email).stream()
                .map(p -> new PlanDto(
                        p.getId(),
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
    @PostMapping
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

        PlanDto dto = new PlanDto(
                saved.getId(),
                saved.getTitle(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getContent(),
                saved.getAuthorEmail()
        );
        URI location = URI.create("/api/plans/" + saved.getId());
        return ResponseEntity.created(location).body(dto);
    }

    /** 단일 플랜 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<PlanDto> getPlan(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan Id: " + id));
        if (!email.equals(p.getAuthorEmail())) {
            return ResponseEntity.status(403).build();
        }
        PlanDto dto = new PlanDto(
                p.getId(),
                p.getTitle(),
                p.getStartDate(),
                p.getEndDate(),
                p.getContent(),
                p.getAuthorEmail()
        );
        return ResponseEntity.ok(dto);
    }

    /** 플랜 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<PlanDto> updatePlan(
            @PathVariable Long id,
            @RequestBody PlanRequestDto req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan Id: " + id));
        if (!email.equals(p.getAuthorEmail())) {
            return ResponseEntity.status(403).build();
        }
        p.setTitle(req.getTitle());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setContent(req.getContent());
        Plan updated = planRepository.save(p);

        PlanDto dto = new PlanDto(
                updated.getId(),
                updated.getTitle(),
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getContent(),
                updated.getAuthorEmail()
        );
        return ResponseEntity.ok(dto);
    }

    /** 플랜 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        Plan p = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan Id: " + id));
        if (!email.equals(p.getAuthorEmail())) {
            return ResponseEntity.status(403).build();
        }
        planRepository.delete(p);
        return ResponseEntity.noContent().build();
    }
}
