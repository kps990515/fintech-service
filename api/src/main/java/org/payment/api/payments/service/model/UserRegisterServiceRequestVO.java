package org.payment.api.payments.service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterServiceRequestVO {
    @NotBlank
    @Email
    private String email;

    @NotEmpty
    private String password;
    private String name;
}
