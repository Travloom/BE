package com.example.travel_project.service;

import com.example.travel_project.dto.PlanDTO;
import com.example.travel_project.dto.PlanRequestDTO;
import com.example.travel_project.entity.Plan;
import com.example.travel_project.entity.User;
import com.example.travel_project.entity.UserPlanList;
import com.example.travel_project.repository.PlanRepository;
import com.example.travel_project.repository.UserPlanListRepository;
import com.example.travel_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserPlanListRepository userPlanListRepository;

    public PlanDTO createPlan (Plan plan) {
        // uuid는 @PrePersist에서 자동 생성됨

        Plan saved = planRepository.save(plan);

        PlanDTO planDTO = PlanDTO.builder()
                .uuid(saved.getUuid())
                .title(saved.getTitle())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .content(saved.getContent())
                .authorEmail(saved.getAuthorEmail())
                .build();

        joinPlan(plan.getUuid(), plan.getAuthorEmail());

        return planDTO;
    }

    public void joinPlan (String uuid, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Plan plan  = planRepository.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (userPlanListRepository.findByPlanId(plan.getId()) == userPlanListRepository.findByUserId(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        UserPlanList userPlanList = UserPlanList.builder()
                .user(user)
                .plan(plan)
                .build();

        userPlanListRepository.save(userPlanList);
    }
}
