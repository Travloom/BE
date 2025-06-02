// DTO for plan data
// src/main/java/com/example/travel_project/dto/PlanDto.java
package com.example.travel_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {
    private String uuid;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String content;
    private String authorEmail;
}
