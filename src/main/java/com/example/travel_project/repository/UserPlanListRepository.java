package com.example.travel_project.repository;

import com.example.travel_project.entity.UserPlanList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPlanListRepository extends JpaRepository<UserPlanList, Long> {

    List<UserPlanList> findByUserId(Long userId);
    List<UserPlanList> findByPlanId(Long planId);
}