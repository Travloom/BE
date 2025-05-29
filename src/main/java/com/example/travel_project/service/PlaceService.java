package com.example.travel_project.service;

import com.example.travel_project.dto.PlaceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {
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
                    return new PlaceDTO(name, description, address, rating, reviews, id, score, latVal, lngVal, null, null, null);
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
                    // description 없음!
                    return new PlaceDTO(name, "", address, rating, reviews, id, score, latVal, lngVal, null, null, null);
                })
                .filter(p -> p.getRating() > 0)
                .filter(p -> EXCLUDE_NAMES.stream().noneMatch(ex -> p.getName().contains(ex)))
                .sorted(Comparator.comparingDouble(PlaceDTO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 이하 기존 geocode, extractAddress 등은 그대로!
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