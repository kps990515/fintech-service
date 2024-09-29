package org.payment.alarm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.payment.common.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserNotificationService {
    private final EmailService emailService;

    @EventListener
    public void handleUserRegisterEvent(UserRegisterEvent event) {
        log.info("Received UserRegisterEvent: email={}", event.getEmail());
        String email = event.getEmail();
        sendEmail(email);

    }

    private void sendEmail(String email) {
        try {
            emailService.sendWelcomeEmailAsync(email);
            System.out.println("비동기 결제 이메일 전송 완료");
        } catch (MailException e) {
            // 예외 처리 로직 추가 (로깅 등)
            System.err.println("이메일 전송 중 오류 발생: " + e.getMessage());
        }
    }
}
