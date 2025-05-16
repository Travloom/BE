package com.example.travel_project.repository;

import com.example.travel_project.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email); // 이메일로 사용자 조회
    Optional<AppUser> findByEmail(String email);
}
