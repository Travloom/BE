package com.example.travel_project.controller;

import com.example.travel_project.dto.PlaceSearchRequestDto;
import com.example.travel_project.dto.PlaceSearchResponseDto;
import com.example.travel_project.service.PlaceService;
import com.example.travel_project.service.ChatGptService;
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
