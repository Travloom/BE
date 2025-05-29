package com.example.travel_project.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DayScheduleDTO {
    private int day;
    private List<ScheduleDTO> schedule;
}
