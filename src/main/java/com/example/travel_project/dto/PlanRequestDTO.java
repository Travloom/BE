// DTO for create/update plan request
// src/main/java/com/example/travel_project/dto/PlanRequestDto.java
package com.example.travel_project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequestDTO {
    private String title;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
    private String content;
}