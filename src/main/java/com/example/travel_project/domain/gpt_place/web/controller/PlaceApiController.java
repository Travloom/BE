package com.example.travel_project.domain.gpt_place.web.controller;

import com.example.travel_project.domain.plan.web.dto.PlanDTO;
import com.example.travel_project.domain.firestore.service.FirestoreService;
import com.example.travel_project.domain.gpt_place.service.PlaceService;
import com.example.travel_project.domain.gpt_place.service.ChatGptService;
import com.example.travel_project.domain.plan.web.dto.PlanRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;


@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceApiController {
    private final PlaceService placeService;
    private final ChatGptService chatGptService;
    private final FirestoreService firestoreService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PlanDTO> searchPlaces(
            @RequestBody PlanRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) throws ExecutionException, InterruptedException {

        // ✏️ "kakao_account" 대신 이미 attributes에 담긴 "email"만 꺼내 씁니다.
        String email = principal.getAttribute("email");
        if (email == null || email.isBlank()) {
            // 이메일 정보가 없으면 인증 실패 처리
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PlanDTO resp = placeService.searchAndBuildPlaces(req, chatGptService, email);
        return ResponseEntity.ok(resp);
    }
}

