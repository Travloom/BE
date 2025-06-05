package com.example.travel_project.domain.gpt_place.web.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class GptScheduleDTO {   // 하루 일정에서의 한 개 일정(타임슬롯) 정보
    private String place;          // 장소명
    private String title;          // 오전,점심,오후,카페,저녁,숙소。。。
    private String content;        // 설명
    private int day;
    private double startTime;      // 시간들 ....
    private double endTime;
}
