package com.example.travel_project.controller;

import com.example.travel_project.dto.PlaceSearchRequestDTO;
import com.example.travel_project.dto.PlanDTO;
import com.example.travel_project.service.FirestoreService;
import com.example.travel_project.service.PlaceService;
import com.example.travel_project.service.ChatGptService;
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

    @Operation(
            summary = "AI 여행 플랜 생성", // Swagger UI에서 API 엔드포인트 옆에 나오는 한줄 설명
            description = "AI(GPT)와 Google API를 활용해 여행 일정을 추천합니다."
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PlanDTO> searchPlaces(
            @RequestBody PlaceSearchRequestDTO req,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) throws ExecutionException, InterruptedException {

        Map<String, Object> kakaoAccound = (Map<String, Object>) principal.getAttribute("kakao_account");

        if (kakaoAccound == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = kakaoAccound.get("email").toString();

        PlanDTO resp = placeService.searchAndBuildPlaces(req, chatGptService, email);
        return ResponseEntity.ok(resp);
    }
}
