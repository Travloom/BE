package com.example.travel_project.domain.plan.repository;

import com.example.travel_project.domain.plan.data.Plan;
import com.example.travel_project.domain.plan.data.mapping.UserPlanList;
import com.example.travel_project.domain.user.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPlanListRepository extends JpaRepository<UserPlanList, Long> {

    List<UserPlanList> findByUserId(Long userId);
    List<UserPlanList> findByPlanId(Long planId);
    Optional<UserPlanList> findByUserAndPlan(User user, Plan plan);


}