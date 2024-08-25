package org.payment.api.payments.controller.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "email은 필수 입력 값입니다.")
    @Email
    private String email;

    @NotEmpty(message = "Password는 필수 입력 값입니다.")
    private String password;
}
