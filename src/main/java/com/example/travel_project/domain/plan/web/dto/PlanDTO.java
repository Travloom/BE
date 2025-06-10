// DTO for plan data
// src/main/java/com/example/travel_project/dto/PlanDto.java
package com.example.travel_project.domain.plan.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {

    @NotNull
    private String uuid;
    private String title;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
    private String content;
    private String authorEmail;

    private TagDTO tags;
}
