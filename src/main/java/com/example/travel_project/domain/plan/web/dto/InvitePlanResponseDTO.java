package com.example.travel_project.domain.plan.web.dto;

import lombok.*;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvitePlanResponseDTO {
    private String userName;
    private String planTitle;
}

