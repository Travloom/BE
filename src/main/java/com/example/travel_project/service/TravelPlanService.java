package com.example.travel_project.service;

import com.example.travel_project.entity.AppUser;
import com.example.travel_project.entity.TravelPlan;
import com.example.travel_project.repository.TravelPlanRepository;
import com.example.travel_project.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TravelPlanService {

    private final TravelPlanRepository planRepo;
    private final UserRepository userRepo;

    public TravelPlanService(TravelPlanRepository planRepo, UserRepository userRepo) {
        this.planRepo = planRepo;
        this.userRepo = userRepo;
    }

    /**
     * 현재 로그인한 사용자의 이메일을 추출하고
     * DB에서 AppUser 엔티티를 조회해 반환
     */
    private AppUser getCurrentUser(OAuth2User oauth2User) {
        // 카카오 계정 정보 맵 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
        String email;

        if (kakaoAccount != null && kakaoAccount.get("email") != null) {
            email = kakaoAccount.get("email").toString();
        } else {
            // 카카오 외에 OAuth2User에 직접 이메일이 있는 경우
            email = oauth2User.getAttribute("email");
        }

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * 로그인한 사용자의 모든 플랜 조회
     */
    public List<TravelPlan> getPlans(OAuth2User oauth2User) {
        AppUser user = getCurrentUser(oauth2User);
        return planRepo.findByUser(user);
    }

    /**
     * 새 여행 플랜 생성
     */
    public TravelPlan createPlan(OAuth2User oauth2User, TravelPlan planData) {
        AppUser user = getCurrentUser(oauth2User);
        planData.setUser(user);
        return planRepo.save(planData);
    }

    /**
     * 특정 플랜 상세 조회 (본인 소유인지 검증)
     */
    public TravelPlan getPlan(Long planId, OAuth2User oauth2User) {
        TravelPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("플랜을 찾을 수 없습니다."));

        // 본인 소유 체크
        AppUser user = getCurrentUser(oauth2User);
        if (!plan.getUser().getEmail().equals(user.getEmail())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }

        return plan;
    }

    // (추가) 수정, 삭제 메서드도 필요에 따라 구현 가능합니다.
}
