package com.example.travel_project.domain.plan.repository;

import com.example.travel_project.domain.plan.data.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByAuthorEmail(String authorEmail);   // 이메일로 플랜 검색
    Optional<Plan> findByUuid(String uuid);
    void deleteByUuid(String uuid);
    boolean existsByUuid(String uuid);

    /**
     * startDate의 날짜 부분만 비교해서 특정 날짜에 출발하는 플랜 조회
     */
    @Query("SELECT p FROM Plan p " +
            " WHERE FUNCTION('date', p.startDate) = :date")
    List<Plan> findByStartDateEqualsDate(@Param("date") LocalDate date);
}
