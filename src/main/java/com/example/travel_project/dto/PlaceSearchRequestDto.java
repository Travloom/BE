package com.example.travel_project.dto;
// DTO for place search request

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchRequestDto {
    private String region;
    private String title;
    private String itinerary;
    private String budget;
    private String people;
    private String companions;
    private String theme;
}