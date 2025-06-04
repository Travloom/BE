package com.example.travel_project.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder
public class TagDTO {
    private String theme;
    private String region;
    private String people;
    private String companions;
}
