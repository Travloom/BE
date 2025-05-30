package com.example.travel_project.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DayScheduleDTO {   // 하루(일자) 단위로 묶은 일정 목록
    private int day;
    private List<ScheduleDTO> schedule;
}
