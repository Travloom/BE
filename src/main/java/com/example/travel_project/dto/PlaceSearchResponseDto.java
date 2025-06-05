// src/main/java/com/example/travel_project/dto/PlaceSearchResponseDto.java
package com.example.travel_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponseDto {    // 여행지 추천/검색 API 응답 전체 포장
    private String region;
    private String itinerary;
    private String companions;
    private String theme;
    private List<PlaceDetailDTO> places;
    private Map<String, String> descriptions;
    private String gptAnswer;
    private List<DayScheduleDTO> schedules;
}
