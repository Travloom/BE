package com.example.travel_project.domain.plan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // application.properties 에 설정된 발신자 계정(Gmail 등)
    @Value("${spring.mail.username}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);  // 수신자 이메일
        msg.setSubject(subject);  // 메일 제목
        msg.setText(text);  //  // 메일 내용
        mailSender.send(msg);
    }
}
