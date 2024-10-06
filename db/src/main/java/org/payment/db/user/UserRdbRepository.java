package org.payment.db.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRdbRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUserId(String userId);

    Optional<UserEntity> findByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.joinedAt <= :thresholdDate")
    List<UserEntity> findUsersToNotify(@Param("thresholdDate") LocalDateTime thresholdDate);
}
