package com.example.travel_project.domain.plan.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlanInfoDTO {
    private TagDTO tags;
    private String title;

    private String startDate;

    private String endDate;
}