package org.payment.db.user;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.payment.db.BaseEntity;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity implements Persistable<String> {
    // Persistable : 엔티티의 신규 상태여부 구분하고 식별자 관리로직 커스터마이징 가능
    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @PrePersist
    // persist전에 기존 uuid체크해서 JPA SAVE시 Select 실행안되도록 함(효율적)
    // persist 연산을 통해 처음으로 데이터베이스에 저장되기 전에 메소드가 실행
    // DB에 처음 저장될때만 실행(Update할때마다 바꾸고 싶으면 @PreUpdate사용)
    private void generateUUID(){
        this.userId = UuidCreator.getTimeOrderedEpoch().toString();
    }

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "email", length = 256, unique = true)
    @Email
    private String email;

    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "email_sent", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isPwModifySednYn = false;

    @Override
    @Transient
    // 새로운 Entity의 경우 insert, 아니면 update
    public boolean isNew() {
        return getCreatedAt() == null || getCreatedAt().equals(getModifiedAt());
    }

    @Override
    // id값 반환해서 신규인지 판단
    public String getId() {
        return this.userId;
    }

}
