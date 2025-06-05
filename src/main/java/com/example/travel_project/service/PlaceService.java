package com.example.travel_project.service;

import com.example.travel_project.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {
    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    // 제외할 프랜차이즈 리스트
    private static final List<String> EXCLUDE_NAMES = List.of(
            "스타벅스", "투썸", "투썸플레이스", "이디야", "커피빈", "빽다방", "컴포즈커피",
            "엔제리너스", "탐앤탐스", "파리바게뜨", "뚜레쥬르", "던킨도너츠", "베스킨라빈스",
            "맥도날드", "버거킹", "롯데리아", "KFC", "BHC", "BBQ",
            "교촌치킨", "굽네치킨", "네네치킨", "페리카나", "도미노피자",
            "피자헛", "미스터피자", "본죽"
    );

    public PlaceSearchResponseDto searchAndBuildPlaces(
            PlaceSearchRequestDto req,
            ChatGptService chatGptService
    ) {
        // 1) 일정 파싱
        int nights = 1, days = 2;
        Matcher itMatch = Pattern.compile("(\\d+)박(\\d+)일").matcher(req.getItinerary());
        if (itMatch.find()) {
            nights = Integer.parseInt(itMatch.group(1));
            days = Integer.parseInt(itMatch.group(2));
        }
        int expectedCount = days * 2 - 1;

        // 2) GPT로 여행지 추천 요청
        StringBuilder placePrompt = new StringBuilder();
        placePrompt.append("아래 조건에 맞춰 여행지를 중복 없이 추천해주세요:\n")
                .append("지역: ").append(req.getRegion()).append("\n")
                .append("누구와: ").append(req.getCompanions()).append("\n")
                .append("테마: ").append(req.getTheme()).append("\n")
                .append("일정: ").append(req.getItinerary()).append("\n")
                .append("추천 여행지 ").append(expectedCount).append("개를 번호와 함께 목록 형식으로 알려주세요. ")
                .append("각 장소에 대해 간단한 설명도 함께 적어주세요. 같은 장소가 중복되지 않도록 해주세요.\n")
                .append("각 장소의 추천은 '누구와', '테마' 정보를 반드시 반영해주세요. 어린이, 가족, 친구, 연인, 휴식, 모험, 맛집 등 특성별로 다른 장소를 추천해야 합니다.\n")
                .append("여행 코스를 짤 때 실제 제공된 장소만 사용해서 짜주세요.\n");
        String placeGptResponse = chatGptService.ask(new ArrayList<>(), placePrompt.toString());

        // 3) GPT 응답 파싱
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

        // 4) GPT 추천 결과에 대해 정확 매칭 → PlaceDTO 생성
        Set<String> usedPlaceIds = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        List<PlaceDTO> attractions = new ArrayList<>();

        for (String name : placeNames) {
            if (usedNames.contains(name)) continue;
            usedNames.add(name);

            // 4-1) 정확 매칭 시도
            PlaceDTO exact = findExactPlace(name, placeDescriptions.getOrDefault(name, ""));
            if (exact != null && !usedPlaceIds.contains(exact.getPlaceId())) {
                usedPlaceIds.add(exact.getPlaceId());
                attractions.add(exact);
            } else {
                // 4-2) 폴백: 키워드 조합 Nearby Search
                PlaceDTO foundDto = searchByKeywordCombination(
                        name,
                        placeDescriptions.getOrDefault(name, ""),
                        req.getRegion(),
                        req.getTheme(),
                        req.getCompanions(),
                        usedPlaceIds
                );
                if (foundDto != null) {
                    usedPlaceIds.add(foundDto.getPlaceId());
                    attractions.add(foundDto);
                }
            }

            if (attractions.size() >= expectedCount) break;
        }

        // 5) 부족 시, 부가 키워드 단계별로 보충
        if (attractions.size() < expectedCount) {
            List<String> fillKeywords = new ArrayList<>();
            if (req.getCompanions() != null && !req.getCompanions().isBlank()
                    && req.getTheme() != null && !req.getTheme().isBlank()) {
                fillKeywords.add(req.getCompanions() + " " + req.getTheme());
            }
            if (req.getTheme() != null && !req.getTheme().isBlank()) {
                fillKeywords.add(req.getTheme());
            }
            if (req.getCompanions() != null && !req.getCompanions().isBlank()) {
                fillKeywords.add(req.getCompanions());
            }
            fillKeywords.add(""); // 빈 문자열(지역만)

            for (String fk : fillKeywords) {
                if (attractions.size() >= expectedCount) break;

                List<PlaceDTO> candidates = searchPlaces(
                        req.getRegion(),
                        "tourist_attraction",
                        expectedCount * 2,
                        fk,
                        ""
                );
                for (PlaceDTO p : candidates) {
                    if (usedPlaceIds.contains(p.getPlaceId()) || usedNames.contains(p.getName())) continue;
                    PlaceDTO fillDto = new PlaceDTO(
                            p.getName(),
                            "", // GPT 설명 없음
                            p.getAddress(),
                            p.getRating(),
                            p.getReviewCount(),
                            p.getPlaceId(),
                            p.getScore(),
                            p.getLat(),
                            p.getLng(),
                            null,
                            null,
                            null
                    );
                    usedPlaceIds.add(fillDto.getPlaceId());
                    usedNames.add(fillDto.getName());
                    attractions.add(fillDto);
                    if (attractions.size() >= expectedCount) break;
                }
            }
        }

        // 6) 주변 식당/카페/숙소 검색
        List<PlaceDetailDTO> placeDetails = new ArrayList<>();
        for (PlaceDTO a : attractions) {
            List<PlaceDTO> restaurants = searchNearby(a.getLat(), a.getLng(), "restaurant", 3);
            List<PlaceDTO> cafes = searchNearby(a.getLat(), a.getLng(), "cafe", 3);
            List<PlaceDTO> hotels = searchNearby(a.getLat(), a.getLng(), "lodging", 3);

            a.setRestaurants(restaurants);
            a.setCafes(cafes);
            a.setHotels(hotels);

            placeDetails.add(new PlaceDetailDTO(a));
        }

        // 7) 일정 생성용 GPT 프롬프트 및 JSON 파싱
        // ───────────────────────────────
        // 프롬프트 작성
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 조건에 맞춰 여행 일정을 JSON 배열 형태로만 출력해 주세요.\n")
                .append("- 반드시 day 1부터 day ").append(days).append("까지 모두 포함할 것! (예: 3박4일이면 day 1, 2, 3, 4)\n")
                .append("- 마지막 날(day ").append(days).append(")은 관광지 1개, 나머지 날은 반드시 관광지 2개씩으로 구성 (1,2,3일차는 2개, 마지막날은 1개)\n")
                .append("- 첫째날은 점심부터 일정 구성(반드시 첫째날에 관광지 2개), 마지막날은 점심까지 일정 구성\n")
                .append("- 일정을 짤 때 아침식사-관광지-점심식사-카페-관광지-저녁식사 이 순서로 해줘.\n")
                .append("- 하루당 아침, 점심, 관광, 카페, 저녁, 숙박, 체크인/체크아웃 등 시간 안겹치게\n")
                .append("- 식당과 카페를 추천할 때는 최근 추천한 관광지 근처로 추천, 이미 추천한 식당/카페는 중복X\n")
                .append("- content는 장소에 대한 한 문장 설명 (단순 \"식사\" 말고 진짜 설명)\n")
                .append("- startTime/endTime는 24시간제 실수 (예: 9.0, 13.5)\n")
                .append("- 같은 day 안에서는 시간이 겹치지 않게\n")
                .append("- 각 스케줄의 place는 장소 이름,\n")
                .append("- title은 '아침', '점심', '관광', '카페', '숙소' 등 역할,\n")
                .append("- content는 그 장소에 대한 짧은 한 문장 설명을 넣을 것\n")
                .append("- 숙소는 첫날만 추천, 나머지 날짜는 추천X\n")
                .append("불필요한 설명, 문장, 마크다운 등 없이, 반드시 아래 예시처럼 출력하세요.\n")
                .append("예시:\n")
                .append("[\n")
                .append("  {\n")
                .append("    \"day\": 1,\n")
                .append("    \"schedule\": [\n")
                .append("      { \"place\": \"경복궁\", \"title\": \"관광\", \"content\": \"조선시대 궁궐.\", \"startTime\": 13, \"endTime\": 14.5 },\n")
                .append("      { \"place\": \"N서울타워\", \"title\": \"관광\", \"content\": \"서울 전망 명소.\", \"startTime\": 15, \"endTime\": 16.5 }\n")
                .append("    ]\n")
                .append("  },\n")
                .append("  ...\n")
                .append("  {\n")
                .append("    \"day\": ").append(days).append(",\n")
                .append("    \"schedule\": [\n")
                .append("      { \"place\": \"북촌한옥마을\", \"title\": \"관광\", \"content\": \"한옥 거리.\", \"startTime\": 10, \"endTime\": 11.5 }\n")
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

        // GPT에게 일정 생성 요청
        String gptScheduleJson = chatGptService.ask(new ArrayList<>(), prompt.toString());

        // JSON 파싱: DayScheduleDTO 리스트로 변환
        List<DayScheduleDTO> schedules = new ArrayList<>();
        try {
            // 1. 실제 JSON 배열만 추출
            int startIdx = gptScheduleJson.indexOf("[");
            int endIdx = gptScheduleJson.lastIndexOf("]");
            if (startIdx >= 0 && endIdx > startIdx) {
                gptScheduleJson = gptScheduleJson.substring(startIdx, endIdx + 1);
            }
            // 2. ObjectMapper로 DayScheduleDTO[] 파싱
            ObjectMapper mapper = new ObjectMapper();
            schedules = Arrays.asList(mapper.readValue(gptScheduleJson, DayScheduleDTO[].class));
        } catch (Exception e) {
            e.printStackTrace();
            schedules = new ArrayList<>();
        }

        // (선택) 일정 설명 파싱
        Map<String, String> descriptions = new LinkedHashMap<>();
        Pattern descPat = Pattern.compile("^\\s*\\d+\\.\\s*([^:]+?):\\s*(.+)$");
        for (String line : gptScheduleJson.split("\\r?\\n")) {
            Matcher m = descPat.matcher(line);
            if (m.find()) {
                descriptions.put(m.group(1).trim(), m.group(2).trim());
            }
        }

        // 8) 최종 응답 DTO 반환
        PlaceSearchResponseDto resp = new PlaceSearchResponseDto(
                req.getRegion(),
                req.getItinerary(),
                req.getCompanions(),
                req.getTheme(),
                placeDetails,
                descriptions,
                gptScheduleJson,
                schedules
        );
        return resp;
    }

    // 2) 정확 매칭: Find Place From Text → PlaceDTO
    private PlaceDTO findExactPlace(String queryName, String gptDesc) {
        String url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json"
                + "?input={q}&inputtype=textquery"
                + "&fields=place_id,name,geometry,formatted_address,rating,user_ratings_total"
                + "&key={key}&language=ko";

        Map<String, String> params = Map.of("q", queryName, "key", apiKey);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class, params);
        if (resp == null) return null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) resp.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;

        Map<String, Object> cand = candidates.get(0);
        String name = (String) cand.get("name");
        String placeId = (String) cand.get("place_id");
        String address = cand.getOrDefault("formatted_address", "").toString();
        double rating = cand.get("rating") instanceof Number
                ? ((Number) cand.get("rating")).doubleValue() : 0.0;
        int reviewCount = cand.get("user_ratings_total") instanceof Number
                ? ((Number) cand.get("user_ratings_total")).intValue() : 0;
        Map<?, ?> geo = (Map<?, ?>) ((Map<?, ?>) cand.get("geometry")).get("location");
        double lat = ((Number) geo.get("lat")).doubleValue();
        double lng = ((Number) geo.get("lng")).doubleValue();
        double score = rating * Math.log(reviewCount + 1);

        return new PlaceDTO(
                name,
                gptDesc,
                address,
                rating,
                reviewCount,
                placeId,
                score,
                lat,
                lng,
                null,
                null,
                null
        );
    }

    // 3) 키워드 조합 Nearby Search → PlaceDTO
    private PlaceDTO searchByKeywordCombination(
            String baseName,
            String gptDesc,
            String region,
            String theme,
            String companions,
            Set<String> usedPlaceIds
    ) {
        List<String> combos = new ArrayList<>();
        String t = (theme != null && !theme.isBlank()) ? theme.trim() : "";
        String c = (companions != null && !companions.isBlank()) ? companions.trim() : "";

        if (!t.isEmpty() && !c.isEmpty()) {
            combos.add(baseName + " " + t + " " + c);
        }
        if (!t.isEmpty()) {
            combos.add(baseName + " " + t);
        }
        if (!c.isEmpty()) {
            combos.add(baseName + " " + c);
        }
        combos.add(baseName);

        for (String kw : combos) {
            List<PlaceDTO> foundList = searchPlaces(region, "tourist_attraction", 5, kw, "");
            for (PlaceDTO src : foundList) {
                if (usedPlaceIds.contains(src.getPlaceId())) continue;

                PlaceDTO dto = new PlaceDTO(
                        src.getName(),
                        gptDesc,
                        src.getAddress(),
                        src.getRating(),
                        src.getReviewCount(),
                        src.getPlaceId(),
                        src.getScore(),
                        src.getLat(),
                        src.getLng(),
                        null,
                        null,
                        null
                );
                return dto;
            }
        }
        return null;
    }

    // 4) Nearby Search → PlaceDTO 리스트
    public List<PlaceDTO> searchPlaces(
            String region, String type, int limit, String keyword, String description
    ) {
        // 1) Geocoding → lat,lng
        String geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json"
                + "?address={region}&key={key}&language=ko";
        Map<String, String> geoParams = Map.of("region", region, "key", apiKey);
        @SuppressWarnings("unchecked")
        Map<String, Object> geoResp = restTemplate.getForObject(geocodeUrl, Map.class, geoParams);
        List<?> geoResults = (List<?>) geoResp.get("results");
        if (geoResults.isEmpty()) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        Map<String, Object> loc = (Map<String, Object>)
                ((Map<String, Object>) ((Map<String, Object>) geoResults.get(0)).get("geometry")).get("location");
        double lat = ((Number) loc.get("lat")).doubleValue();
        double lng = ((Number) loc.get("lng")).doubleValue();

        // 2) Nearby Search 반복 호출 (페이징)
        List<Map<String, Object>> allResults = new ArrayList<>();
        String nextPageToken = null;
        do {
            String placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                    + "?location={lat},{lng}"
                    + "&radius=5000&type={type}"
                    + (keyword != null && !keyword.isBlank() ? "&keyword={keyword}" : "")
                    + "&key={key}&language=ko"
                    + (nextPageToken != null ? "&pagetoken={token}" : "");

            Map<String, Object> params = new HashMap<>();
            params.put("lat", lat);
            params.put("lng", lng);
            params.put("type", type);
            params.put("key", apiKey);
            if (keyword != null && !keyword.isBlank()) {
                params.put("keyword", keyword);
            }
            if (nextPageToken != null) {
                params.put("token", nextPageToken);
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(placesUrl, Map.class, params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            allResults.addAll(results);
            Object tokenObj = resp.get("next_page_token");
            nextPageToken = (tokenObj != null) ? tokenObj.toString() : null;
        } while (nextPageToken != null && allResults.size() < limit);

        // 3) 매핑·필터링·정렬 후 DTO 생성
        return allResults.stream()
                .map(m -> {
                    String name = (String) m.get("name");
                    String address = (String) m.getOrDefault("vicinity", "");
                    double rating = m.get("rating") instanceof Number
                            ? ((Number) m.get("rating")).doubleValue() : 0.0;
                    int reviewCount = m.get("user_ratings_total") instanceof Number
                            ? ((Number) m.get("user_ratings_total")).intValue() : 0;
                    String id = (String) m.get("place_id");
                    Map<?, ?> geo = (Map<?, ?>) ((Map<?, ?>) m.get("geometry")).get("location");
                    double latVal = ((Number) geo.get("lat")).doubleValue();
                    double lngVal = ((Number) geo.get("lng")).doubleValue();
                    double score = rating * Math.log(reviewCount + 1);

                    return new PlaceDTO(
                            name,
                            "", // description은 빈 문자열
                            address,
                            rating,
                            reviewCount,
                            id,
                            score,
                            latVal,
                            lngVal,
                            null,
                            null,
                            null
                    );
                })
                .filter(p -> p.getRating() > 0)
                .filter(p -> EXCLUDE_NAMES.stream().noneMatch(ex -> p.getName().contains(ex)))
                .sorted(Comparator.comparingDouble(PlaceDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 5) 주변 식당/카페/숙소 검색
    public List<PlaceDTO> searchNearby(double lat, double lng, String type, int limit) {
        List<Map<String, Object>> allResults = new ArrayList<>();
        String nextPageToken = null;

        do {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                    + "?location={lat},{lng}&radius=2000&type={type}"
                    + "&key={key}&language=ko"
                    + (nextPageToken != null ? "&pagetoken={token}" : "");

            Map<String, Object> params = new HashMap<>();
            params.put("lat", lat);
            params.put("lng", lng);
            params.put("type", type);
            params.put("key", apiKey);
            if (nextPageToken != null) {
                params.put("token", nextPageToken);
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class, params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            allResults.addAll(results);
            Object tokenObj = resp.get("next_page_token");
            nextPageToken = (tokenObj != null) ? tokenObj.toString() : null;
        } while (nextPageToken != null && allResults.size() < limit);

        return allResults.stream()
                .map(m -> {
                    String name = (String) m.get("name");
                    String address = (String) m.getOrDefault("vicinity", "");
                    double rating = m.get("rating") instanceof Number
                            ? ((Number) m.get("rating")).doubleValue() : 0.0;
                    int reviewCount = m.get("user_ratings_total") instanceof Number
                            ? ((Number) m.get("user_ratings_total")).intValue() : 0;
                    String id = (String) m.get("place_id");
                    double score = rating * Math.log(reviewCount + 1);
                    Map<?, ?> locMap = (Map<?, ?>) ((Map<?, ?>) m.get("geometry")).get("location");
                    double latVal = ((Number) locMap.get("lat")).doubleValue();
                    double lngVal = ((Number) locMap.get("lng")).doubleValue();

                    return new PlaceDTO(
                            name,
                            "",
                            address,
                            rating,
                            reviewCount,
                            id,
                            score,
                            latVal,
                            lngVal,
                            null,
                            null,
                            null
                    );
                })
                .filter(p -> p.getRating() > 0)
                .filter(p -> EXCLUDE_NAMES.stream().noneMatch(ex -> p.getName().contains(ex)))
                .sorted(Comparator.comparingDouble(PlaceDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
