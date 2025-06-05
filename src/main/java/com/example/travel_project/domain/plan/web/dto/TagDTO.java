package com.example.travel_project.domain.plan.web.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagDTO {
    private String theme;
    private String region;
    private String people;
    private String companions;
}
