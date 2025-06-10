package com.example.travel_project.domain.plan.data.mapping;

import com.example.travel_project.domain.plan.data.Plan;
import com.example.travel_project.domain.user.data.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_plan_list")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlanList {

    /** Surrogate PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User ↔ UserPlanList N:1 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** Plan ↔ UserPlanList N:1 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Plan plan;

    // (필요하다면 추가 속성 e.g. 참여일, 승인여부 등)
}