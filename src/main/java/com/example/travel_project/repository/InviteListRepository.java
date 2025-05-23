package com.example.travel_project.repository;

import com.example.travel_project.entity.InviteList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InviteListRepository extends JpaRepository<InviteList, Long> {
    List<InviteList> findByUserId(Long userId);
    List<InviteList> findByPlanId(Long planId);
}