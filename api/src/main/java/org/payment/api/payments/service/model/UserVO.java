package org.payment.api.payments.service.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class UserVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // 직렬화 버전 관리용 ID
    private String id;
    private String name;
    private String password;
}
