package com.backend.commondb.user;

import com.backend.commondb.common.BaseEntity;
import com.backend.commondb.user.enums.LoginType;
import com.backend.commondb.user.enums.UserType;
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
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(name = "sso_provider")
    private String ssoProvider;

    @Column(name = "sso_subject_id")
    private String ssoSubjectId;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;
}
