package com.example.travel_project.dto;
// DTO for user profile

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private String name;
    private String profileImageUrl;
    private String email;
}