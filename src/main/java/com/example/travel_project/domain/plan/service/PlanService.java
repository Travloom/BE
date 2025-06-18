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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    public List<PlanDTO> getPlans (String email, LocalDateTime before, LocalDateTime after, Integer year, Integer month) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));
        List<UserPlanList> userPlanLists = user.getUserPlanLists();
        List<Plan> plans = userPlanLists.stream()
                .map(UserPlanList::getPlan)
                .toList();

        Stream<Plan> filteredPlans = plans.stream();

        if (before != null) {
            filteredPlans = filteredPlans.filter(p -> p.getEndDate().isBefore(before));
        }

        else if (after != null) {
            filteredPlans = filteredPlans.filter(p -> !p.getEndDate().isBefore(after));
        }

        else if (year != null && month != null) {
            LocalDateTime startOfPrevMonth = LocalDateTime.of(year, month, 1, 1, 1, 0, 0)
                    .minusMonths(1);
            LocalDateTime endOfNextMonth = LocalDateTime.of(year, month, 1, 1, 1, 0, 0)
                    .plusMonths(1)                // 다음 달로 이동
                    .withDayOfMonth(1)            // 1일로 설정
                    .plusMonths(1)                // 한 달 더 이동 (2달 뒤)
                    .minusDays(1);

            filteredPlans = filteredPlans.filter(p ->
                    (p.getStartDate() != null && !p.getStartDate().isBefore(startOfPrevMonth) && !p.getStartDate().isAfter(endOfNextMonth)) ||
                            (p.getEndDate() != null && !p.getEndDate().isBefore(startOfPrevMonth) && !p.getEndDate().isAfter(endOfNextMonth))
            );
        }

        return filteredPlans.map(p -> PlanDTO.builder()
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
    }

    public void joinPlan (String uuid, String inviteeEmail) {
        User user = userRepository.findByEmail(inviteeEmail).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대할 유저가 존재하지 않습니다."));
        Plan plan  = planRepository.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플랜이 존재하지 않습니다."));

        if (userPlanListRepository.findByUserAndPlan(user, plan).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여중인 유저입니다.");
        }

        UserPlanList userPlanList = UserPlanList.builder()
                .user(user)
                .plan(plan)
                .build();

        userPlanListRepository.save(userPlanList);
    }

    public Boolean isCollaborator (String uuid, String email) {
        Plan plan = planRepository.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 플랜입니다."));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));
        return userPlanListRepository.findByUserAndPlan(user, plan).isPresent();
    }
}
