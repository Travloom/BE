package com.example.travel_project.domain.gpt_place.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceDTO {   // 여행지(관광지/식당/카페/숙소 등) 한 곳의 정보
    private String name;
    private String address;
    private double rate;
    private String photoReference;
    private int reviewCount;     // 총 리뷰 수
    private String placeId;
    private double score;        // 로그 가중 점수
    private double lat;
    private double lng;
    private List<String> types;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceDTO)) return false;
        PlaceDTO that = (PlaceDTO) o;
        return Objects.equals(placeId, that.placeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeId);
    }
}
