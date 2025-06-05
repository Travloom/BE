package com.example.travel_project.domain.gpt_place.web.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@Builder
@AllArgsConstructor
public class PlaceListsDTO {
    private List<PlaceDTO> attractionList;
    private List<PlaceDTO> restaurantList;
    private List<PlaceDTO> cafeList;
    private List<PlaceDTO> hotelList;
}
