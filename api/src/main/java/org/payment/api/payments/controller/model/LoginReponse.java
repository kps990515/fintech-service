package org.payment.api.payments.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginReponse {
    private String email;
    private String name;
    private String password;
}
