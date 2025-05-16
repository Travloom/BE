package com.example.travel_project.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AppUser { // 또는 Users
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long number;

    private String nickname;
    private String profileImageUrl;
    private String email;

    public AppUser() {
    }

    public AppUser(String nickname, String profileImageUrl, String email) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}