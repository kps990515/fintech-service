package org.payment.api.payments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    @Async
    //(TODO) UserService 개별함수로 들어갔을때에도 async로 동작할 것인가
    public void sendWelcomeEmailAsync(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("환영합니다!");
            message.setText("회원 가입을 축하드립니다!");

            emailSender.send(message);
            System.out.println("비동기 이메일 전송 완료");
        } catch (MailException e) {
            // 예외 처리 로직 추가 (로깅 등)
            System.err.println("이메일 전송 중 오류 발생: " + e.getMessage());
        }
    }
}
