package com.backend.commondataaccess.persistence.user;

import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "users_authentication")
@Entity
public class UserAuthentication extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "login_provider")
    private LoginProvider loginProvider; // LOCAL, GOOGLE, KAKAO 등

    // ID
    @Column(name = "identifier")
    private String identifier; // 로그인 아이디. (직접 가입이면 email/ID, OAuth면 subject_id)

    // PASSWORD
    @Column(name = "credential")
    private String credential; // 직접 가입일 때만 존재. (암호화된 password). OAuth일 때는 null이 됩니다.

    public void updateCredential(String credential) {
        this.credential = credential;
    }

    public void updateUser(User user) {
        this.user = user;
    }
}
