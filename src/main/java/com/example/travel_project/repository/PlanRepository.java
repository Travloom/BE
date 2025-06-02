package com.example.travel_project.repository;

import com.example.travel_project.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByAuthorEmail(String authorEmail);   // 이메일로 플랜 검색
    Optional<Plan> findByUuid(String uuid);
    void deleteByUuid(String uuid);
    boolean existsByUuid(String uuid);
}
