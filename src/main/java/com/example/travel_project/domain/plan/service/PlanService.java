package com.example.travel_project.domain.plan.service;

import com.example.travel_project.domain.firestore.service.FirestoreService;
import com.example.travel_project.domain.gpt_place.web.dto.PlaceListsDTO;
import com.example.travel_project.domain.gpt_place.web.dto.ScheduleListWrapperDTO;
import com.example.travel_project.domain.plan.web.dto.PlanDTO;
import com.example.travel_project.domain.plan.data.Plan;
import com.example.travel_project.domain.plan.web.dto.PlanInfoDTO;
import com.example.travel_project.domain.plan.web.dto.TagDTO;
import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.plan.data.mapping.UserPlanList;
import com.example.travel_project.domain.plan.repository.PlanRepository;
import com.example.travel_project.domain.plan.repository.UserPlanListRepository;
import com.example.travel_project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.ExecutionException;


@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserPlanListRepository userPlanListRepository;
    private final FirestoreService firestoreService;

    public PlanDTO createPlan (Plan plan) throws ExecutionException, InterruptedException {
        // uuid는 @PrePersist에서 자동 생성됨

        Plan saved = planRepository.save(plan);

        PlanDTO planDTO = PlanDTO.builder()
                .uuid(saved.getUuid())
                .title(saved.getTitle())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .content(saved.getContent())
                .authorEmail(saved.getAuthorEmail())
                .tags(TagDTO.builder()
                        .region(saved.getRegion())
                        .people(saved.getPeople())
                        .companions(saved.getCompanions())
                        .theme(saved.getTheme())
                        .build())
                .build();

        PlanInfoDTO planInfo = PlanInfoDTO.builder()
                .authorEmail(saved.getAuthorEmail())
                .title(saved.getTitle())
                .startDate(saved.getStartDate().toString())
                .endDate(saved.getEndDate().toString())
                .tags(TagDTO.builder()
                        .region(saved.getRegion())
                        .people(saved.getPeople())
                        .companions(saved.getCompanions())
                        .theme(saved.getTheme())
                        .build())
                .build();

        PlaceListsDTO placeLists = PlaceListsDTO.builder()
                .attractionList(List.of())
                .cafeList(List.of())
                .hotelList(List.of())
                .restaurantList(List.of())
                .build();

        ScheduleListWrapperDTO scheduleList = ScheduleListWrapperDTO.builder()
                .scheduleList(List.of())
                .build();

        firestoreService.savePlanData(plan.getUuid(), "info", planInfo);
        firestoreService.savePlanData(plan.getUuid(), "places", placeLists);
        firestoreService.savePlanData(plan.getUuid(), "schedules", scheduleList);

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
