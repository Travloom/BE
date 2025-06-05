package com.example.travel_project.dto;
// DTO for place search request

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchRequestDto {   //여행지 추천/검색 API에서 사용자 요청 값 전달
    private String region;
    private String title;
    private String itinerary;
    private String companions;
    private String theme;
}