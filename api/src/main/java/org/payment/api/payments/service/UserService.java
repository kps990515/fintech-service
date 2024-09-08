package org.payment.api.payments.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.payment.api.common.exception.ExistUserFoundException;
import org.payment.api.common.exception.InvalidSessionException;
import org.payment.api.common.exception.UserNotFoundException;
import org.payment.api.common.util.ObjectConvertUtil;
import org.payment.api.config.web.interceptor.JpaQueryInterceptor;
import org.payment.api.payments.controller.model.LoginRequest;
import org.payment.api.payments.service.mapper.UserMapper;
import org.payment.api.payments.service.model.UserRegisterServiceRequestVO;
import org.payment.api.payments.service.model.UserVO;
import org.payment.db.user.UserEntity;
import org.payment.db.user.UserRdbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRdbRepository userRdbRepository;
    private final UserMapper userMapper;
    private final EmailService emailService; // 예시로 이메일 발송 서비스
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    public void login(LoginRequest loginRequest, HttpSession httpSession){
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        // 1. 사용자 찾기
        UserEntity userEntity = userRdbRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new); // 사용자를 찾지 못했을 때 예외 발생

        UserVO userVO = userMapper.toUserVO(userEntity);

        // 2. 사용자 정보 일치하는지 확인
        if (userVO.getPassword().equals(password)) {
            // 세션에 사용자 정보 저장 (Redis로 자동 저장)
            httpSession.setAttribute("USER_SESSION", userVO);
        } else {
            throw new InvalidSessionException("유저정보가 일치하지 않습니다");
        }
    }

    public UserVO getCurrentUser(HttpSession httpSession) {
        UserVO userVO = ObjectConvertUtil.copyVO(httpSession.getAttribute("USER_SESSION"), UserVO.class);
        if (userVO.getId() == null) {
            throw new InvalidSessionException();
        }
        return userVO;
    }

    public String registerUser(UserRegisterServiceRequestVO requestVO) {
        // try-with-resources사용해서 entityManager & Hibernate Session/Interceptor 생성
        try (EntityManager entityManager = entityManagerFactory.createEntityManager();
             Session session = entityManager.unwrap(Session.class).getSessionFactory().withOptions()
                     .statementInspector(new JpaQueryInterceptor())  // 특정 세션에 StatementInspector 적용
                     .openSession()) {

            Transaction transaction = session.beginTransaction();  // 트랜잭션 시작

            userRdbRepository.findByEmail(requestVO.getEmail())
                    .ifPresent(user -> {
                        throw new ExistUserFoundException();
                    });

            UserEntity newUser = userMapper.toUserEntity(requestVO);
            session.persist(newUser);  // Hibernate 세션을 통해 저장

            transaction.commit();  // 트랜잭션 커밋

            // 비동기 이메일 발송
            emailService.sendWelcomeEmailAsync(newUser.getEmail());

            return requestVO.getEmail();

        } catch (Exception e) {
            // 트랜잭션 롤백
            throw new RuntimeException("Error during registration", e);
        }
    }
}
