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
    @JoinColumn(name = "user_id", nullable = false)   // 초대 받은 사람 id
    private User user;

    /** Plan ↔ UserPlanList N:1 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)  // plan 테이블에서 원래 id번호
    private Plan plan;


}