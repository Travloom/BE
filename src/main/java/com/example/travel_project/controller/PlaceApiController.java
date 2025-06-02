package com.example.travel_project.controller;

import com.example.travel_project.dto.PlaceSearchRequestDto;
import com.example.travel_project.dto.PlaceSearchResponseDto;
import com.example.travel_project.service.PlaceService;
import com.example.travel_project.service.ChatGptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceApiController {

    private final PlaceService placeService;
    private final ChatGptService chatGptService;

    @Operation(
            summary = "AI 여행 플랜 생성", // Swagger UI에서 API 엔드포인트 옆에 나오는 한줄 설명
            description = "AI(GPT)와 Google API를 활용해 여행 일정을 추천합니다."
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PlaceSearchResponseDto> searchPlaces(
            @RequestBody PlaceSearchRequestDto req
    ) {
        PlaceSearchResponseDto resp = placeService.searchAndBuildPlaces(req, chatGptService);
        return ResponseEntity.ok(resp);
    }
}
