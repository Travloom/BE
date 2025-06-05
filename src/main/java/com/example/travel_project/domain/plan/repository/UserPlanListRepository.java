package com.example.travel_project.domain.plan.repository;

import com.example.travel_project.domain.plan.data.mapping.UserPlanList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPlanListRepository extends JpaRepository<UserPlanList, Long> {

    List<UserPlanList> findByUserId(Long userId);
    List<UserPlanList> findByPlanId(Long planId);
}