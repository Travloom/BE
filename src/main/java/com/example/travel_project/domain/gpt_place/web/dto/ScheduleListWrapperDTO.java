package com.example.travel_project.domain.gpt_place.web.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class ScheduleListWrapperDTO {   // 하루 일정에서의 한 개 일정(타임슬롯) 정보
    private List<ScheduleDTO> scheduleList;
}
