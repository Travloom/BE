// DTO for create/update plan request
// src/main/java/com/example/travel_project/dto/PlanRequestDto.java
package com.example.travel_project.domain.plan.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequestDTO {
    private String title;

    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String content;

    private String region;
    private String people;
    private String companions;
    private String theme;
}