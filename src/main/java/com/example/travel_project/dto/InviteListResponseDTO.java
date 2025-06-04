package com.example.travel_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteListResponseDTO {
    private Long id;
    private String userName;
    private String userEmail;
    private String planTitle;
    private Boolean isAccepted;
}

