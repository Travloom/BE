package com.example.travel_project.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class ScheduleDTO {
    private String place;          // 장소명
    private String title;          // 오전,점심,오후,카페,저녁,숙소。。。
    private String content;        // 설명
    private double startTime;      // 시간들 ....
    private double endTime;
}
