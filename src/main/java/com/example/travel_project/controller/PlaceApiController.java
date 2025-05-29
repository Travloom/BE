package com.example.travel_project.controller;

import com.example.travel_project.dto.*;
import com.example.travel_project.service.ChatGptService;
import com.example.travel_project.service.PlaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // 1) 일정 파싱
        int nights = 1, days = 2;
        Matcher itMatch = Pattern.compile("(\\d+)박(\\d+)일").matcher(req.getItinerary());
        if (itMatch.find()) {
            nights = Integer.parseInt(itMatch.group(1));
            days   = Integer.parseInt(itMatch.group(2));
        }
        int expectedCount = days * 2 - 1;

        // 2) GPT로 여행지 추천 요청
        StringBuilder placePrompt = new StringBuilder();
        placePrompt.append("아래 조건에 맞춰 여행지를 중복 없이 추천해주세요:\n")
                .append("지역: ").append(req.getRegion()).append("\n")
                .append("일정: ").append(req.getItinerary()).append("\n")
                .append("예산: ").append(req.getBudget()).append("\n")
                .append("인원: ").append(req.getPeople()).append("\n")
                .append("누구와: ").append(req.getCompanions()).append("\n")
                .append("테마: ").append(req.getTheme()).append("\n")
                .append("추천 여행지 ").append(expectedCount).append("개를 번호와 함께 목록 형식으로 알려주세요. 각 장소에 대해 간단한 설명도 함께 적어주세요. 같은 장소가 중복되지 않도록 해주세요.")
                .append("여행 코스를 짤 때 실제 제공된 장소만 사용해서 짜주세요.\n");
        String placeGptResponse = chatGptService.ask(new ArrayList<>(), placePrompt.toString());

        // 3) GPT 응답 파싱: 장소 이름·설명 추출
        List<String> placeNames = new ArrayList<>();
        Map<String, String> placeDescriptions = new LinkedHashMap<>();
        Pattern namePat = Pattern.compile("^\\s*\\d+\\.\\s*([^:]+):?\\s*(.*)$");
        for (String line : placeGptResponse.split("\\r?\\n")) {
            Matcher m = namePat.matcher(line);
            if (m.find()) {
                String name = m.group(1).trim();
                String desc = m.group(2).trim();
                placeNames.add(name);
                placeDescriptions.put(name, desc);
            }
        }

        // 4) 중복 없이 여행지 추가 (GPT 추천 기반)
        Set<String> usedPlaceIds = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        List<PlaceDTO> attractions = new ArrayList<>();

        for (String name : placeNames) {
            if (usedNames.contains(name)) continue;
            usedNames.add(name);

            List<PlaceDTO> foundList = placeService.searchPlaces(
                    req.getRegion(), "tourist_attraction", 1, name, placeDescriptions.getOrDefault(name, "")
            );
            PlaceDTO place = null;
            if (!foundList.isEmpty()) {
                PlaceDTO found = foundList.get(0);
                // placeId 없는 관광지는 추가하지 않음
                if (found.getPlaceId() == null || usedPlaceIds.contains(found.getPlaceId())) continue;
                usedPlaceIds.add(found.getPlaceId());

                place = new PlaceDTO(
                        found.getName(),
                        "",
                        found.getAddress(),
                        found.getRating(),
                        found.getReviewCount(),
                        found.getPlaceId(),
                        found.getScore(),
                        found.getLat(),
                        found.getLng(),
                        new ArrayList<>(), // restaurants
                        new ArrayList<>(), // cafes
                        new ArrayList<>()  // hotels
                );
                attractions.add(place);
            }
            if (attractions.size() >= expectedCount) break;
        }


        // 5) 부족할 경우, 지역 인기 장소로 보충
        if (attractions.size() < expectedCount) {
            List<PlaceDTO> candidates = placeService.searchPlaces(
                    req.getRegion(),
                    "tourist_attraction",
                    expectedCount * 2, // 여유 있게 받아옴
                    "", ""
            );
            for (PlaceDTO p : candidates) {
                String idKey = p.getPlaceId();
                if (usedPlaceIds.contains(idKey) || usedNames.contains(p.getName())) continue;
                attractions.add(p);
                usedPlaceIds.add(idKey);
                usedNames.add(p.getName());
                if (attractions.size() >= expectedCount) break;
            }
        }

        // 6) 관광지별 restaurants, cafes, hotels를 attraction에 세팅해서 래핑
        List<PlaceDetailDTO> placeDetails = new ArrayList<>();
        for (PlaceDTO a : attractions) {
            List<PlaceDTO> restaurants = placeService.searchNearby(a.getLat(), a.getLng(), "restaurant", 3);
            List<PlaceDTO> cafes = placeService.searchNearby(a.getLat(), a.getLng(), "cafe", 3);
            List<PlaceDTO> hotels = placeService.searchNearby(a.getLat(), a.getLng(), "lodging", 3);

            a.setRestaurants(restaurants);
            a.setCafes(cafes);
            a.setHotels(hotels);

            placeDetails.add(new PlaceDetailDTO(a));
        }

        // 7) 실제 장소 리스트를 GPT 프롬프트에 삽입하여 일정 짜기 (JSON 배열로만 출력 강력 요구)
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 조건에 맞춰 여행 일정을 JSON 배열 형태로만 출력해 주세요.\n")
                .append("- 반드시 day 1부터 day ").append(days).append("까지 모두 포함할 것! (예: 4박5일이면 day 1, 2, 3, 4, 5)\n")
                .append("- 첫째날은 점심부터 일정 구성, 마지막날은 점심까지 일정 구성해줘. 그리고 아침식사는 9시 이후부터 하는걸로\n")
                .append("- 하루당 아침, 점심, 관광, 카페, 저녁, 숙박, 체크인/체크아웃 등 시간 안겹치게\n")
                .append("- 식당과 카페를 추천할 때는 최근 관광지 근처로 추천해줘\n")
                .append("- content는 장소에 대한 한 문장 설명 (단순 \"식사\" 말고 진짜 설명)\n")
                .append("- startTime/endTime은 24시간제 실수로 (예: 9.0, 13.5)\n")
                .append("- 같은 day 안에서는 시간이 겹치지 않게\n")
                .append("- 각 스케줄의 place는 장소 이름,\n")
                .append("- title은 '아침', '점심', '관광', '카페', '체크인', '숙소' 등 역할,\n")
                .append("- content는 그 장소에 대한 짧은 한 문장 설명을 넣을 것\n")
                .append("- 숙소는 첫날만 추천하고 나머지 날짜는 추천하지마\n")
                .append("불필요한 설명, 문장, 마크다운 등 없이, 반드시 아래 예시처럼 출력하세요.\n")
                .append("예시:\n")
                .append("[\n")
                .append("  {\n")
                .append("    \"day\": 1,\n")
                .append("    \"schedule\": [\n")
                .append("      {\n")
                .append("        \"place\": \"형제칼국수\",\n")
                .append("        \"title\": \"점심\",\n")
                .append("        \"content\": \"강릉의 대표적인 음식인 칼국수를 맛볼 수 있는 식당입니다.\",\n")
                .append("        \"startTime\": 12,\n")
                .append("        \"endTime\": 13.5\n")
                .append("      }\n")
                .append("    ]\n")
                .append("  }\n")
                .append("]\n")
                .append("아래 정보만 사용해 일정을 짜세요.\n")
                .append("[관광지 목록]\n");
        for (PlaceDetailDTO d : placeDetails) {
            prompt.append("- ").append(d.getAttraction().getName()).append("\n");
        }
        prompt.append("[식당 목록]\n");
        for (PlaceDetailDTO d : placeDetails) {
            for (PlaceDTO r : Optional.ofNullable(d.getAttraction().getRestaurants()).orElse(Collections.emptyList())) {
                prompt.append("- ").append(r.getName()).append("\n");
            }
        }
        prompt.append("[카페 목록]\n");
        for (PlaceDetailDTO d : placeDetails) {
            for (PlaceDTO c : Optional.ofNullable(d.getAttraction().getCafes()).orElse(Collections.emptyList())) {
                prompt.append("- ").append(c.getName()).append("\n");
            }
        }
        prompt.append("[숙소 목록]\n");
        for (PlaceDetailDTO d : placeDetails) {
            for (PlaceDTO h : Optional.ofNullable(d.getAttraction().getHotels()).orElse(Collections.emptyList())) {
                prompt.append("- ").append(h.getName()).append("\n");
            }
        }

        String gptScheduleJson = chatGptService.ask(new ArrayList<>(), prompt.toString());

        // 8) GPT에서 받은 JSON 배열을 파싱 (DayScheduleDTO 리스트)
        List<DayScheduleDTO> schedules = new ArrayList<>();
        try {
            // 1. GPT 응답이 진짜 JSON인지 확인!
            System.out.println("GPT SCHEDULE RAW: " + gptScheduleJson);

            // 2. JSON만 추출
            int startIdx = gptScheduleJson.indexOf("[");
            int endIdx = gptScheduleJson.lastIndexOf("]");
            if (startIdx >= 0 && endIdx > startIdx) {
                gptScheduleJson = gptScheduleJson.substring(startIdx, endIdx + 1);
            }

            // 3. 파싱 시도
            ObjectMapper mapper = new ObjectMapper();
            schedules = Arrays.asList(mapper.readValue(gptScheduleJson, DayScheduleDTO[].class));
        } catch (Exception e) {
            e.printStackTrace(); // 반드시 에러 원인 로그!
            schedules = new ArrayList<>();
        }

        // 9) GPT 설명 파싱 (선택사항)
        Map<String, String> descriptions = new LinkedHashMap<>();
        Pattern descPat = Pattern.compile("^\\s*\\d+\\.\\s*([^:]+?):\\s*(.+)$");
        for (String line : gptScheduleJson.split("\\r?\\n")) {
            Matcher m = descPat.matcher(line);
            if (m.find()) {
                descriptions.put(m.group(1).trim(), m.group(2).trim());
            }
        }

        // 10) 응답 DTO 조립 및 반환
        PlaceSearchResponseDto resp = new PlaceSearchResponseDto(
                req.getRegion(),
                req.getItinerary(),
                req.getBudget(),
                req.getPeople(),
                req.getCompanions(),
                req.getTheme(),
                placeDetails,
                descriptions,
                gptScheduleJson,
                schedules
        );
        return ResponseEntity.ok(resp);
    }
}
