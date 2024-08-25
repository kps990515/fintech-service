package org.payment.api.payments.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.payment.api.common.exception.InvalidSessionException;
import org.payment.api.common.util.ObjectConvertUtil;
import org.payment.api.payments.controller.model.LoginReponse;
import org.payment.api.payments.controller.model.LoginRequest;
import org.payment.api.payments.controller.model.UserRegisterRequest;
import org.payment.api.payments.service.UserService;
import org.payment.api.payments.service.model.UserRegisterServiceRequestVO;
import org.payment.api.payments.service.model.UserVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("account")
public class AccountManageCotroller {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/v1/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest, HttpSession httpSession){
        userService.login(loginRequest, httpSession);
        return ResponseEntity.ok("로그인 성공");
    }

    @GetMapping("/v1/me")
    public ResponseEntity<LoginReponse> me(HttpServletRequest request){
        HttpSession httpSession = request.getSession(false); // 세션이 없으면 null 반환
        if (httpSession == null) {
            throw new InvalidSessionException();
        }

        String sessionId = httpSession.getId();
        UserVO userVO = (UserVO) redisTemplate.opsForValue().get("USER_SESSION:" + sessionId);

        if (userVO == null) {
            throw new InvalidSessionException();
        }

        LoginReponse response = ObjectConvertUtil.copyVO(userVO, LoginReponse.class);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/logout")
    public ResponseEntity<String> logout(HttpSession httpSession) {
        String sessionId = httpSession.getId();
        String redisKey = "USER_SESSION:" + sessionId;

        // Redis에서 세션 데이터 삭제
        redisTemplate.delete(redisKey);

        // HttpSession 무효화
        httpSession.invalidate();

        return ResponseEntity.ok("로그아웃 완료");
    }

    @PostMapping("/v1/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        UserRegisterServiceRequestVO svo = ObjectConvertUtil.copyVO(userRegisterRequest, UserRegisterServiceRequestVO.class);
        String userEmail = userService.registerUser(svo);
        return ResponseEntity.ok(String.format("가입완료 : %s", userEmail));
    }
}
