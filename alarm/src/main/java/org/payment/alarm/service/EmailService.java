package org.payment.alarm.service;

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
    public void sendPaymentConfirmEmailAsync(String paymentKey, String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("결제완료");
            message.setText("payment Key" + paymentKey + "결제완료");

            emailSender.send(message);
            System.out.println("비동기 결제 이메일 전송 완료");
        } catch (MailException e) {
            // 예외 처리 로직 추가 (로깅 등)
            System.err.println("결제 이메일 전송 중 오류 발생: " + e.getMessage());
        }
    }

    @Async
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

