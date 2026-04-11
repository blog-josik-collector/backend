package com.backend.commondataaccess.persistence.user;

import com.backend.commondataaccess.persistence.BaseEntity;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_type", nullable = false)
    private UserType userType;

    private String nickname;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    public void update(String nickname) {
        this.nickname = nickname;
    }

    public void login() {
        this.lastLoginAt = OffsetDateTime.now();
    }
}
