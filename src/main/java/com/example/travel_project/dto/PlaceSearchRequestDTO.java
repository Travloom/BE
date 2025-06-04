package com.example.travel_project.dto;
// DTO for place search request

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchRequestDTO {   //여행지 추천/검색 API에서 사용자 요청 값 전달
    private String title;
    private String region;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
    private String people;
    private String companions;
    private String theme;
}