package com.example.travel_project.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    PLACES("places"),
    SCHEDULE("schedules");

    private final String key;
}
