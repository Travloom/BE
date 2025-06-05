package com.example.travel_project.domain.gpt_place.web.dto;
// DTO for place search request

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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