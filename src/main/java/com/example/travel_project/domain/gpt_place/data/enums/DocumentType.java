package com.example.travel_project.domain.gpt_place.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    PLACES("places"),
    SCHEDULE("schedules");

    private final String key;
}
