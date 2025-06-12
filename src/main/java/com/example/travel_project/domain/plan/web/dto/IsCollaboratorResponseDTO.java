package com.example.travel_project.domain.plan.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class IsCollaboratorResponseDTO {

    @JsonProperty("isExist")
    boolean isExist;

    @JsonProperty("isCollaborator")
    boolean isCollaborator;
}
