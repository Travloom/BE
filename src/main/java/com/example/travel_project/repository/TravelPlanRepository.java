package com.example.travel_project.repository;

import com.example.travel_project.entity.TravelPlan;
import com.example.travel_project.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {
    // 특정 사용자(user)에게 속한 모든 플랜 조회
    List<TravelPlan> findByUser(AppUser user);
}
