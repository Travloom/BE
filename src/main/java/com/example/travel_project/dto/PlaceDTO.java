package com.example.travel_project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceDTO {   // 여행지(관광지/식당/카페/숙소 등) 한 곳의 정보
    private String name;
    private String description;
    private String address;
    private double rating;
    private int reviewCount;     // 총 리뷰 수
    private String placeId;
    private double score;        // 로그 가중 점수
    private double lat;
    private double lng;

    private List<PlaceDTO> restaurants;
    private List<PlaceDTO> cafes;
    private List<PlaceDTO> hotels;
}
