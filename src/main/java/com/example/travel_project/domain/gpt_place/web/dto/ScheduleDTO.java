package com.example.travel_project.domain.gpt_place.web.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ScheduleDTO {

    private String title;
    private String content;
    private String i;

    private int x;
    private int y;
    private int w;

    @Builder.Default
    private int h = 1;

    @Builder.Default
    private Boolean isBounded = false;

    @Builder.Default
    private Boolean isDraggable = true;

    @Builder.Default
    private Boolean isResizable = true;

    @Builder.Default
    private int maxH = 1;

    @Builder.Default
    private int maxW = 48;

    @Builder.Default
    private int minH = 1;

    @Builder.Default
    private int minW = 1;

    @Builder.Default
    private Boolean moved = true;

    @Builder.Default
    private List<String> resizeHandles = new ArrayList<>(List.of("e"));
}

