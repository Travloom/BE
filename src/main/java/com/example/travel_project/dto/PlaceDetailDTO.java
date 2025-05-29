package com.example.travel_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PlaceDetailDTO {   // 출력 형태 변경
    private PlaceDTO attraction;
}
