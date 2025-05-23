package com.example.travel_project.controller;

import com.example.travel_project.dto.PlaceDTO;
import com.example.travel_project.entity.Plan;
import com.example.travel_project.service.ChatGptService;
import com.example.travel_project.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final ChatGptService chatGptService;

    @GetMapping("/places")
    public String showForm(Model model) {
        model.addAttribute("region", "");
        model.addAttribute("itinerary", "");
        model.addAttribute("budget", "");
        model.addAttribute("people", "");
        model.addAttribute("companions", "");
        model.addAttribute("theme", "");
        model.addAttribute("planToSave", new Plan());

        // 빈칸 방지
        model.addAttribute("attractions", Collections.emptyList());
        model.addAttribute("restaurantsNear", Collections.emptyMap());
        model.addAttribute("cafesNear", Collections.emptyMap());
        model.addAttribute("descriptions", Collections.emptyMap());

        return "places";
    }

    @PostMapping("/places")
    public String search(
            @RequestParam String region,
            @RequestParam(required = false, defaultValue = "") String title,
            @RequestParam(required = false, defaultValue = "") String itinerary,
            @RequestParam(required = false, defaultValue = "") String budget,
            @RequestParam(required = false, defaultValue = "") String people,
            @RequestParam(name = "companions", required = false, defaultValue = "") String companions,
            @RequestParam(required = false, defaultValue = "") String theme,
            Model model
    ) {
        // 1) 일정 파싱: “X박Y일”
        int nights = 1, days = 2;
        Matcher itMatch = Pattern.compile("(\\d+)박(\\d+)일").matcher(itinerary);
        if (itMatch.find()) {
            nights = Integer.parseInt(itMatch.group(1));
            days = Integer.parseInt(itMatch.group(2));
        }

        // 2) 관광지 검색
        List<PlaceDTO> attractions = placeService.searchPlaces(region, "tourist_attraction", 2 * days - 1, theme);
        int totalAttractions = attractions.size();


        // 3) 맛집·카페 검색
        Map<String, List<PlaceDTO>> restaurantsNear = new HashMap<>();
        Map<String, List<PlaceDTO>> cafesNear = new HashMap<>();
        for (PlaceDTO a : attractions) {
            restaurantsNear.put(
                    a.getPlaceId(),
                    placeService.searchNearby(a.getLat(), a.getLng(), "restaurant", 3)
            );
            cafesNear.put(
                    a.getPlaceId(),
                    placeService.searchNearby(a.getLat(), a.getLng(), "cafe", 3)
            );
        }

        // 4) ChatGPT 프롬프트 생성
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 여행 조건에 맞춰 여행 코스를 추천해주시고,\n")
                .append("반드시 각각의 여행지에 대한 짧은 설명도 써주세요\n")
                .append("1. 여행지 이름 : (그 여행지에 대한 설명)…\n")
                .append("2. …\n\n")
                .append("여행지 정보에 null이거 절대 안뜨게 반드시 각각의 여행지에 대한 짧은 설명도 써주세요\n")
                .append("각 추천 관광명소 주변의 맛집과 카페도 함께 알려주세요.\n\n")
                .append("지역: ").append(region).append("\n")
                .append("일정: ").append(itinerary).append("\n")
                .append("예산: ").append(budget).append("\n")
                .append("인원: ").append(people).append("\n")
                .append("동행: ").append(companions).append("\n")
                .append("테마: ").append(theme).append("\n\n")
                .append("추천 관광명소 총 ").append(attractions.size()).append("곳:\n\n");

        // 일정별로 나누기
        int baseCount = totalAttractions / days;
        int rem = totalAttractions % days;
        List<Integer> perDay = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            perDay.add(baseCount + (i < rem ? 1 : 0));
        }

        int idx = 0;
        for (int day = 1; day <= days; day++) {
            int cnt = Math.min(perDay.get(day - 1), totalAttractions - idx);
            prompt.append(day).append("일차 관광명소 (").append(cnt).append("곳):\n");
            for (int j = 0; j < cnt; j++) {
                PlaceDTO a = attractions.get(idx);
                prompt.append(" ").append(idx + 1).append(". ").append(a.getName())
                        .append(" (주소: ").append(a.getAddress()).append(")\n")
                        .append("   주변 맛집:\n");
                for (PlaceDTO r : restaurantsNear.get(a.getPlaceId())) {
                    prompt.append("     - ").append(r.getName())
                            .append(" — 평점: ").append(r.getRating())
                            .append(", 주소: ").append(r.getAddress()).append("\n");
                }
                prompt.append("   주변 카페:\n");
                for (PlaceDTO c : cafesNear.get(a.getPlaceId())) {
                    prompt.append("     - ").append(c.getName())
                            .append(" — 평점: ").append(c.getRating())
                            .append(", 주소: ").append(c.getAddress()).append("\n");
                }
                prompt.append("\n");
                idx++;
            }
        }

        // 5) ChatGPT API 호출
        String gptAnswer = chatGptService.ask(new ArrayList<>(), prompt.toString());

        // 6) GPT 응답 파싱: "1. 장소명: 설명" → descriptions 맵
        Map<String, String> descriptions = new LinkedHashMap<>();
        Pattern descPat = Pattern.compile("^\\s*\\d+\\.\\s*([^:]+?):\\s*(.+)$");
        for (String line : gptAnswer.split("\\r?\\n")) {
            Matcher m = descPat.matcher(line);
            if (m.find()) {
                descriptions.put(m.group(1).trim(), m.group(2).trim());
            }
        }

        // 7) Model에 담기
        model.addAttribute("region", region);
        model.addAttribute("itinerary", itinerary);
        model.addAttribute("budget", budget);
        model.addAttribute("people", people);
        model.addAttribute("companions", companions);
        model.addAttribute("theme", theme);

        model.addAttribute("attractions", attractions);
        model.addAttribute("restaurantsNear", restaurantsNear);
        model.addAttribute("cafesNear", cafesNear);

        model.addAttribute("gptAnswer", gptAnswer);
        model.addAttribute("descriptions", descriptions);

        // 8) Plan 저장용: content에 전체 GPT 응답 담기
        Plan planToSave = new Plan();
        planToSave.setContent(gptAnswer);
        planToSave.setTitle(title);
        model.addAttribute("planToSave", planToSave);

        return "places";
    }
}
