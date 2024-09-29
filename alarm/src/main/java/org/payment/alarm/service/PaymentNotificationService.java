package org.payment.alarm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.payment.common.PaymentConfirmedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentNotificationService {
    private final EmailService emailService;

    @EventListener
    public void handlePaymentConfirmedEvent(PaymentConfirmedEvent event) {
        log.info("Received PaymentConfirmedEvent: paymentKey={}, email={}", event.getPaymentKey(), event.getEmail());
        String paymentKey = event.getPaymentKey();
        String email = event.getEmail();
        System.out.println("Payment confirmed for key: " + paymentKey + ", email: " + email);

        sendEmail(paymentKey, email);

    }

    private void sendEmail(String paymentKey, String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("결제가 완료되었습니다");

            emailService.sendPaymentConfirmEmailAsync(paymentKey, email);
            System.out.println("비동기 결제 이메일 전송 완료");
        } catch (MailException e) {
            // 예외 처리 로직 추가 (로깅 등)
            System.err.println("결제 이메일 전송 중 오류 발생: " + e.getMessage());
        }
    }
}
