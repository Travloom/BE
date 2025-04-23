package com.example.travel_project.repository;

import com.example.travel_project.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email); // 이메일로 사용자 조회
}
