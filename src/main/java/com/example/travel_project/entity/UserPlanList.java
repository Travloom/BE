package com.example.travel_project.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private User user;

    /** Plan ↔ UserPlanList N:1 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // (필요하다면 추가 속성 e.g. 참여일, 승인여부 등)
}