package org.payment.api.payments.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.payment.api.common.util.ObjectConvertUtil;
import org.payment.api.common.exception.ExistUserFoundException;
import org.payment.api.common.exception.InvalidSessionException;
import org.payment.api.common.exception.UserNotFoundException;
import org.payment.api.payments.controller.model.LoginRequest;
import org.payment.api.payments.service.mapper.UserMapper;
import org.payment.api.payments.service.model.UserRegisterServiceRequestVO;
import org.payment.api.payments.service.model.UserVO;
import org.payment.db.user.UserEntity;
import org.payment.db.user.UserRdbRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRdbRepository userRdbRepository;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public void login(LoginRequest loginRequest, HttpSession httpSession){
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        // 1. 사용자 찾기
        UserEntity userEntity = userRdbRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new); // 사용자를 찾지 못했을 때 예외 발생

        UserVO userVO = userMapper.toUserVO(userEntity);

        // 2. 사용자 정보 일치하는지 확인
        if (userVO.getPassword().equals(password)) {
            //세션 저장
            httpSession.setAttribute("USER_SESSION", userVO);
            // Redis 저장
            String sessionId = httpSession.getId();
            String redisKey = "USER_SESSION:" + sessionId;
            redisTemplate.opsForValue().set(redisKey, userVO, 1800L, TimeUnit.SECONDS);
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
        userRdbRepository.findByEmail(requestVO.getEmail())
                .ifPresent(user -> {
                    throw new ExistUserFoundException();
                });

        UserEntity newUser = userMapper.toUserEntity(requestVO);
        userRdbRepository.save(newUser);

        return requestVO.getEmail();
    }
}
