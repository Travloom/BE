package com.example.travel_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String API_URL    = "https://api.openai.com/v1/chat/completions";
    private static final int    MAX_HISTORY = 8;  // 보낼 과거 메시지 개수 제한


    public String ask(List<Map<String,String>> chatHistory, String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        // 1) HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2) 메시지 조립: system + 최근 대화(MAX_HISTORY) + 이번 질문
        List<Map<String,String>> messages = new ArrayList<>();

        // (A) system 프롬프트
        messages.add(Map.of(
                "role",    "system",
                "content", "You are a helpful assistant."
        ));

        // (B) 슬라이딩 윈도우: chatHistory 중 최신 MAX_HISTORY개만
        int historySize = chatHistory.size();
        int start = Math.max(0, historySize - MAX_HISTORY);
        if (historySize > 0) {
            messages.addAll(chatHistory.subList(start, historySize));
        }

        // (C) 이번 사용자 메시지
        messages.add(Map.of(
                "role",    "user",
                "content", userMessage
        ));

        // 3) 요청 바디 구성
        Map<String,Object> body = new HashMap<>();
        body.put("model", "gpt-4");  // 테스트 끝나면 gpt-3.5-turbo -> gpt-4로 변경
        body.put("messages",    messages);
        body.put("max_tokens",  2000);
        body.put("temperature", 0.1);   // 낮으면 정확도, 일관성   높으면 창의성, 다양한 표현


        // 4) API 호출
        HttpEntity<Map<String,Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                API_URL, HttpMethod.POST, requestEntity, Map.class
        );

        // 5) 응답 파싱
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<?> choices = (List<?>) response.getBody().get("choices");
            if (!choices.isEmpty()) {
                Map<?,?> choice = (Map<?,?>) choices.get(0);
                Map<?,?> message = (Map<?,?>) choice.get("message");
                return message.get("content").toString().trim();
            }
        }

        // 오류 시 기본 메시지
        return "죄송합니다, 답변 생성 중 오류가 발생했습니다.";
    }
}