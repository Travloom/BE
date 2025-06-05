package com.example.travel_project.domain.user.data;

import com.example.travel_project.domain.plan.data.mapping.UserPlanList;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Builder
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    private String email;

    @Builder.Default
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