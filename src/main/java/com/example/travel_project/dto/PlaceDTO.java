package com.example.travel_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaceDTO {
    private String name;
    private String address;
    private double rating;
    private int reviewCount;     // 총 리뷰 수
    private String placeId;
    private double score;        // 로그 가중 점수
    private double lat;
    private double lng;
}
