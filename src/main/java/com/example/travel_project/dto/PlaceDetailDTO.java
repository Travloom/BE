package com.example.travel_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PlaceDetailDTO {   // attraction 래핑용
    private PlaceDTO attraction;
}
