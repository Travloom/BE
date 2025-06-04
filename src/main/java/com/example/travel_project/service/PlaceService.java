package com.example.travel_project.service;

import com.example.travel_project.dto.*;
import com.example.travel_project.entity.Plan;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.threeten.bp.temporal.Temporal;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlanService planService;
    private final FirestoreService firestoreService;
    private final ObjectMapper objectMapper;
    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    // 제외할 프렌차이즈 리스트
    private static final List<String> EXCLUDE_NAMES = List.of(
            "스타벅스", "투썸", "투썸플레이스", "이디야", "커피빈", "빽다방", "컴포즈커피",
            "엔제리너스", "탐앤탐스", "파리바게뜨", "뚜레쥬르", "던킨도너츠", "베스킨라빈스",
            "맥도날드", "버거킹", "롯데리아", "KFC", "BHC", "BBQ",
            "교촌치킨", "굽네치킨", "네네치킨", "페리카나", "도미노피자",
            "피자헛", "미스터피자", "본죽"
    );

    // [★ 핵심 로직 메서드 ★]
    public PlanDTO searchAndBuildPlaces(
            PlaceSearchRequestDTO req,
            ChatGptService chatGptService,
            String email
    ) throws ExecutionException, InterruptedException {
        // 1) 일정 파싱
        LocalDateTime startDate = req.getStartDate();
        LocalDateTime endDate = req.getEndDate();

        System.out.println("출발 : " + startDate);
        System.out.println("도착 : " + endDate);

        int days = (int)ChronoUnit.DAYS.between(startDate, endDate) + 1;

        System.out.println(days + "일");

        int expectedCount = days * 2 - 1;

        // 2) GPT로 여행지 추천 요청
        StringBuilder placePrompt = new StringBuilder();
        placePrompt.append("아래 조건에 맞춰 여행지를 중복 없이 추천해주세요:\n")
                .append("지역: ").append(req.getRegion()).append("\n")
                .append("여행 출발일: ").append(req.getStartDate()).append("\n")
                .append("여행 종료일: ").append(req.getEndDate()).append("\n")
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
        List<PlaceDTO> attractionList = new ArrayList<>();

        for (String name : placeNames) {
            if (usedNames.contains(name)) continue;
            usedNames.add(name);

            List<PlaceDTO> foundList = searchPlaces(
                    req.getRegion(), "", 1, name, placeDescriptions.getOrDefault(name, "")
            );
            PlaceDTO place = null;
            if (!foundList.isEmpty()) {
                PlaceDTO found = foundList.get(0);
                // placeId 없는 관광지는 추가하지 않음
                if (found.getPlaceId() == null || usedPlaceIds.contains(found.getPlaceId())) continue;
                usedPlaceIds.add(found.getPlaceId());

                place = PlaceDTO.builder()
                        .name(found.getName())
                        .address(found.getAddress())
                        .rating(found.getRating())
                        .imageUrl(found.getImageUrl())
                        .reviewCount(found.getReviewCount())
                        .placeId(found.getPlaceId())
                        .score(found.getScore())
                        .lat(found.getLat())
                        .lng(found.getLng())
                        .types(found.getTypes())
                        .imageUrl(found.getImageUrl())
                        .build();
                attractionList.add(place);
            }
            if (attractionList.size() >= expectedCount) break;
        }

        // 5) 부족할 경우, 지역 인기 장소로 보충
        if (attractionList.size() < expectedCount) {
            List<PlaceDTO> candidates = searchPlaces(
                    req.getRegion(),
                    "tourist_attraction",
                    expectedCount * 2, // 여유 있게 받아옴
                    "", ""
            );
            for (PlaceDTO p : candidates) {
                String idKey = p.getPlaceId();
                if (usedPlaceIds.contains(idKey) || usedNames.contains(p.getName())) continue;
                attractionList.add(p);
                usedPlaceIds.add(idKey);
                usedNames.add(p.getName());
                if (attractionList.size() >= expectedCount) break;
            }
        }

        // 6) restaurants, cafes, hotels를 저장
        Set<PlaceDTO> restaurantSet = new HashSet<>();
        Set<PlaceDTO> cafeSet = new HashSet<>();
        Set<PlaceDTO> hotelSet = new HashSet<>();

        for (PlaceDTO a : attractionList) {
            List<PlaceDTO> restaurants = searchNearby(a.getLat(), a.getLng(), "restaurant", 3);
            List<PlaceDTO> cafes = searchNearby(a.getLat(), a.getLng(), "cafe", 3);
            List<PlaceDTO> hotels = searchNearby(a.getLat(), a.getLng(), "lodging", 3);

            restaurantSet.addAll(restaurants);
            cafeSet.addAll(cafes);
            hotelSet.addAll(hotels);
        }

        List<PlaceDTO> restaurantList = new ArrayList<>(restaurantSet);
        List<PlaceDTO> cafeList = new ArrayList<>(cafeSet);
        List<PlaceDTO> hotelList = new ArrayList<>(hotelSet);

        PlaceListsDTO placeLists = PlaceListsDTO.builder()
                .attractionList(attractionList)
                .restaurantList(restaurantList)
                .cafeList(cafeList)
                .hotelList(hotelList)
                .build();

        // 7) 실제 장소 리스트를 GPT 프롬프트에 삽입하여 일정 짜기 (JSON 배열로만 출력 강력 요구)
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 조건에 맞춰 여행 일정을 JSON 배열 형태로만 출력해 주세요.\n")
                .append("- 반드시 day 1부터 day ").append(days).append("까지 모두 포함할 것! (예: 4박5일이면 day 1, 2, 3, 4, 5)\n")
                .append("- 첫째날은 점심부터 일정 구성, 마지막날은 점심까지 일정 구성해줘. 그리고 아침식사는 9시 이후부터 하는걸로\n")
                .append("- 하루당 아침, 점심, 관광, 카페, 저녁, 숙박, 체크인/체크아웃 등 시간 안겹치게\n")
                .append("- 식당과 카페를 추천할 때는 최근 추천한 관광지 근처로 추천해줘. 그리고 이미 추천한 식당,카페를 중복해서 일정에 나오게 하지마.\n")
                .append("- content는 장소에 대한 한 문장 설명 (단순 \"식사\" 말고 진짜 설명)\n")
                .append("- startTime/endTime은 24시간제 실수로 (예: 9.0, 13.5)\n")
                .append("- 같은 day 안에서는 시간이 겹치지 않게\n")
                .append("- 각 스케줄의 place는 장소 이름,\n")
                .append("- title은 '아침', '점심', '관광', '카페', '체크인', '숙소' 등 역할,\n")
                .append("- content는 그 장소에 대한 짧은 한 문장 설명을 넣을 것\n")
                .append("- 숙소는 첫날만 추천하고 나머지 날짜는 추천하지마\n")
                .append("불필요한 설명, 문장, 마크다운 등 없이, 반드시 아래 예시처럼 출력하세요.\n")
                .append("예시:\n")
                .append("{\n")
                .append("    \"schedules\": [\n")
                .append("      {\n")
                .append("        \"place\": \"형제칼국수\",\n")
                .append("        \"title\": \"점심\",\n")
                .append("        \"content\": \"강릉의 대표적인 음식인 칼국수를 맛볼 수 있는 식당입니다.\",\n")
                .append("        \"day\": 1,\n")
                .append("        \"startTime\": 12,\n")
                .append("        \"endTime\": 13.5\n")
                .append("      }\n")
                .append("    ]\n")
                .append("}\n")
                .append("아래 정보만 사용해 일정을 짜세요.\n")
                .append("[관광지 목록]\n");
        for (PlaceDTO placeDTO : placeLists.getAttractionList()) {
            prompt.append("- ").append(placeDTO.getName()).append("\n");
        }
        prompt.append("[식당 목록]\n");
        for (PlaceDTO placeDTO : placeLists.getRestaurantList()) {
            prompt.append("- ").append(placeDTO.getName()).append("\n");
        }
        prompt.append("[카페 목록]\n");
        for (PlaceDTO placeDTO : placeLists.getCafeList()) {
            prompt.append("- ").append(placeDTO.getName()).append("\n");
        }
        prompt.append("[숙소 목록]\n");
        for (PlaceDTO placeDTO : placeLists.getHotelList()) {
            prompt.append("- ").append(placeDTO.getName()).append("\n");
        }

        String gptScheduleJson = chatGptService.ask(new ArrayList<>(), prompt.toString());

        // 8) GPT에서 받은 JSON 배열을 파싱 (DayScheduleDTO 리스트)
        List<GptScheduleDTO> schedules = new ArrayList<>();
        try {
            // 1. GPT 응답이 진짜 JSON인지 확인!
//            System.out.println("GPT SCHEDULE RAW: " + gptScheduleJson);

            // 2. JSON만 추출
            int startIdx = gptScheduleJson.indexOf("{");
            int endIdx = gptScheduleJson.lastIndexOf("}");
            if (startIdx >= 0 && endIdx > startIdx) {
                gptScheduleJson = gptScheduleJson.substring(startIdx, endIdx + 1);
            }

            JsonNode root = objectMapper.readTree(gptScheduleJson);
            JsonNode schedulesNode = root.get("schedules");

            // 3. 파싱 시도
            ObjectMapper objectMapper = new ObjectMapper();
            schedules = objectMapper.readValue(
                    schedulesNode.toString(),
                    new TypeReference<List<GptScheduleDTO>>() {}
            );

        } catch (Exception e) {
            e.printStackTrace(); // 반드시 에러 원인 로그!
            schedules = new ArrayList<>();
        }

        ScheduleListWrapperDTO scheduleList = ScheduleListWrapperDTO.builder()
                .scheduleList(
                        schedules.stream()
                                .map(schedule -> ScheduleDTO.builder()
                                        .title(schedule.getTitle())
                                        .content(schedule.getPlace() + " : " + schedule.getContent())
                                        .i(UUID.randomUUID().toString())
                                        .x((int)(schedule.getStartTime() * 2))
                                        .y(schedule.getDay() - 1)
                                        .w((int)((schedule.getEndTime() - schedule.getStartTime()) * 2))
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();

        Plan plan = Plan.builder()
                .title(req.getTitle())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .authorEmail(email)
                .build();

        PlanDTO planDTO = planService.createPlan(plan);

        PlanInfoDTO planInfo = PlanInfoDTO.builder()
                .title(plan.getTitle())
                .startDate(plan.getStartDate().toString())
                .endDate(plan.getEndDate().toString())
                .tags(TagDTO.builder()
                        .region(req.getRegion())
                        .people(req.getPeople())
                        .companions(req.getCompanions())
                        .theme(req.getTheme())
                        .build())
                .build();

        firestoreService.savePlanData(plan.getUuid(), "info", planInfo);
        firestoreService.savePlanData(plan.getUuid(), "places", placeLists);
        firestoreService.savePlanData(plan.getUuid(), "schedules", scheduleList);

        return planDTO;
    }

    // description 인자 추가!
    public List<PlaceDTO> searchPlaces(String region, String type, int limit, String keyword, String description) {
        // 1) Geocoding
        String geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json"
                + "?address={region}&key={key}&language=ko";
        Map<String, String> geoParams = Map.of("region", region, "key", apiKey);
        @SuppressWarnings("unchecked")
        Map<String, Object> geoResp = restTemplate.getForObject(geocodeUrl, Map.class, geoParams);
        List<?> geoResults = (List<?>) geoResp.get("results");
        if (geoResults.isEmpty()) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        Map<String, Object> loc = (Map<String, Object>) ((Map<String, Object>)
                ((Map<String, Object>) geoResults.get(0)).get("geometry")).get("location");
        double lat = ((Number) loc.get("lat")).doubleValue();
        double lng = ((Number) loc.get("lng")).doubleValue();

        // 2) Nearby Search (theme → keyword)
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
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(placesUrl, Map.class, params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            allResults.addAll(results);
            Object tokenObj = resp.get("next_page_token");
            nextPageToken = (tokenObj != null) ? tokenObj.toString() : null;
        } while (nextPageToken != null && allResults.size() < limit);

        // 3) 매핑·필터링·정렬
        return allResults.stream()
                .map(m -> {
                    String name = (String) m.get("name");
                    String address = (String) m.getOrDefault("vicinity", "");
                    double rating = m.get("rating") instanceof Number
                            ? ((Number) m.get("rating")).doubleValue() : 0.0;
                    int reviews = m.get("user_ratings_total") instanceof Number
                            ? ((Number) m.get("user_ratings_total")).intValue() : 0;
                    String id = (String) m.get("place_id");
                    Map<?, ?> geo = (Map<?, ?>) ((Map<?, ?>) m.get("geometry")).get("location");
                    double latVal = ((Number) geo.get("lat")).doubleValue();
                    double lngVal = ((Number) geo.get("lng")).doubleValue();
                    double score = rating * Math.log(reviews + 1);
                    List<String> types = new ArrayList<>();
                    Object rawTypes = m.get("types");
                    if (rawTypes instanceof List<?>) {
                        for (Object placeType : (List<?>) rawTypes) {
                            if (placeType instanceof String) {
                                types.add((String) placeType);
                            }
                        }
                    }
                    String imageUrl = "";
                    Object rawPhotos = m.get("photos");
                    if (rawPhotos instanceof List<?> && !((List<?>) rawPhotos).isEmpty()) {
                        Object firstPhoto = ((List<?>) rawPhotos).get(0);
                        if (firstPhoto instanceof Map<?, ?>) {
                            Object ref = ((Map<?, ?>) firstPhoto).get("photo_reference");
                            if (ref instanceof String) {
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400"
                                        + "&photo_reference=" + ref
                                        + "&key=" + apiKey;
                            }
                        }
                    }
                    return PlaceDTO.builder()
                            .name(name)
                            .address(address)
                            .rating(rating)
                            .reviewCount(reviews)
                            .placeId(id)
                            .score(score)
                            .lat(latVal)
                            .lng(lngVal)
                            .types(types)
                            .imageUrl(imageUrl)
                            .build();

                })
                .filter(p -> p.getRating() > 0)
                .filter(p -> EXCLUDE_NAMES.stream().noneMatch(ex -> p.getName().contains(ex)))
                .sorted(Comparator.comparingDouble(PlaceDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

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
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
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
                    int reviews = m.get("user_ratings_total") instanceof Number
                            ? ((Number) m.get("user_ratings_total")).intValue() : 0;
                    String id = (String) m.get("place_id");
                    double score = rating * Math.log(reviews + 1);
                    Map<?, ?> locMap = (Map<?, ?>) ((Map<?, ?>) m.get("geometry")).get("location");
                    double latVal = ((Number) locMap.get("lat")).doubleValue();
                    double lngVal = ((Number) locMap.get("lng")).doubleValue();
                    Object rawTypes = m.get("types");
                    List<String> types = new ArrayList<>();
                    if (rawTypes instanceof List<?>) {
                        for (Object placeType : (List<?>) rawTypes) {
                            if (placeType instanceof String) {
                                types.add((String) placeType);
                            }
                        }
                    }
                    String imageUrl = "";
                    Object rawPhotos = m.get("photos");
                    if (rawPhotos instanceof List<?> && !((List<?>) rawPhotos).isEmpty()) {
                        Object firstPhoto = ((List<?>) rawPhotos).get(0);
                        if (firstPhoto instanceof Map<?, ?>) {
                            Object ref = ((Map<?, ?>) firstPhoto).get("photo_reference");
                            if (ref instanceof String) {
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400"
                                        + "&photo_reference=" + ref
                                        + "&key=" + apiKey;
                            }
                        }
                    }
                    // description 없음!
                    return PlaceDTO.builder()
                            .name(name)
                            .address(address)
                            .rating(rating)
                            .reviewCount(reviews)
                            .placeId(id)
                            .score(score)
                            .lat(latVal)
                            .lng(lngVal)
                            .types(types)
                            .imageUrl(imageUrl)
                            .build();
                })
                .filter(p -> p.getRating() > 0)
                .filter(p -> EXCLUDE_NAMES.stream().noneMatch(ex -> p.getName().contains(ex)))
                .sorted(Comparator.comparingDouble(PlaceDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Double> geocode(String query) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json"
                + "?address={q}&key={key}&language=ko";
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(
                url, Map.class, Map.of("q", query, "key", apiKey)
        );
        List<?> results = (List<?>) resp.get("results");
        if (results.isEmpty()) return Map.of("lat", 0.0, "lng", 0.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> loc = (Map<String, Object>)
                ((Map<?, ?>) ((Map<?, ?>) results.get(0)).get("geometry")).get("location");
        return Map.of(
                "lat", ((Number) loc.get("lat")).doubleValue(),
                "lng", ((Number) loc.get("lng")).doubleValue()
        );
    }

    public String extractAddress(String query) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json"
                + "?address={q}&key={key}&language=ko";
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(
                url, Map.class, Map.of("q", query, "key", apiKey)
        );
        List<?> results = (List<?>) resp.get("results");
        if (results.isEmpty()) return "";
        return (String) ((Map<?, ?>) results.get(0)).get("formatted_address");
    }

    public Map<String, Object> findPlaceByName(String query) {
        String url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json"
                + "?input={q}&inputtype=textquery&fields=place_id,name,geometry,formatted_address,rating,user_ratings_total"
                + "&key={key}&language=ko";
        Map<String, String> params = Map.of("q", query, "key", apiKey);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class, params);

        List<?> candidates = (List<?>) resp.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null; // 못찾으면 null
        return (Map<String, Object>) candidates.get(0); // 첫 후보 리턴
    }
}
