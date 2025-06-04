package com.example.travel_project.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPlanList> userPlanLists = new ArrayList<>();

    public User() {
    }

    public User(String name, String profileImageUrl, String email) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
    }

}