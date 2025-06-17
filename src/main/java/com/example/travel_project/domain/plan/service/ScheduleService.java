package com.example.travel_project.domain.plan.service;

import com.example.travel_project.domain.plan.data.Plan;
import com.example.travel_project.domain.plan.data.mapping.UserPlanList;
import com.example.travel_project.domain.plan.repository.PlanRepository;
import com.example.travel_project.domain.plan.repository.UserPlanListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleService {
    private final PlanRepository planRepository;
    private final UserPlanListRepository userPlanListRepository;
    private final EmailService emailService;

    /**
     * 매일 18:00 실행 (Asia/Seoul)
     * 내일 날짜와 일치하는 플랜만 조회
     */
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void sendDayBeforeReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("[Schedule] 내일({}) 전송 시작", tomorrow);

        List<Plan> plans = planRepository.findByStartDateEqualsDate(tomorrow);
        log.info("내일({}) 플랜 조회: {}건", tomorrow, plans.size());

        for (Plan plan : plans) {
            String dateLabel = plan.getStartDate().toLocalDate().toString();
            String title     = plan.getTitle();
            String subject = String.format(
                    "[떠나,봄] 내일 \"%s\" 여행이 곧 시작됩니다 – 준비되셨나요?",
                    title
            );
            String body      = String.format(
                    "안녕하세요,\n\n내일(%s) \"%s\" 일정이 예정되어 있습니다.\n즐거운 여행 되세요! 😊",
                    dateLabel, title
            );

            List<UserPlanList> participants =
                    userPlanListRepository.findByPlanId(plan.getId());
            for (UserPlanList upl : participants) {
                emailService.sendSimpleMessage(
                        upl.getUser().getEmail(),
                        subject,
                        body
                );
            }
        }
    }
}
