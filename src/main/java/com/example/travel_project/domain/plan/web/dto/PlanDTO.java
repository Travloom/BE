// DTO for plan data
// src/main/java/com/example/travel_project/dto/PlanDto.java
package com.example.travel_project.domain.plan.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {

    @NotNull
    private String uuid;
    private String title;

    private ZonedDateTime startDate;
    private ZonedDateTime endDate;

    private String content;
    private String authorEmail;

    private TagDTO tags;
}
