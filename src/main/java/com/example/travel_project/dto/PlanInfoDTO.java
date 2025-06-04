package com.example.travel_project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Builder
@AllArgsConstructor
public class PlanInfoDTO {
    private TagDTO tags;
    private String title;

    private String startDate;

    private String endDate;
}