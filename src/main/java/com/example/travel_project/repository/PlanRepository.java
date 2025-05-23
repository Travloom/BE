package com.example.travel_project.repository;

import com.example.travel_project.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByAuthorEmail(String authorEmail);
}
