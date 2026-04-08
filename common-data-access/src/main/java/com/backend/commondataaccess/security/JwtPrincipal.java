package com.backend.commondataaccess.security;

import com.backend.commondataaccess.security.jwt.JwtService;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 역할 <p> - 컨트롤러/서비스에서 꺼내 쓰는 “인증된 사용자 정보(Claims 기반 Principal)”를 표현하는 도메인-친화적인 Principal 객체. <p> - 컨트롤러/서비스에서 Authentication.getPrincipal()로 꺼내 쓰는 대상. <p> 책임 <p> - 애플리케이션 코드가 필요로 하는 최소 사용자
 * 식별자/표시정보/roles 보관 <p> - Claims로부터 principal 생성(from(Claims)) <p> 주의점 <p> - from(User)도 존재하므로, 이 클래스는 “JWT 기반 인증”뿐 아니라 “User 도메인에서 principal로 매핑”에도 쓰일 수 있음 <p> - 팀 규칙을 정해서 “이 메서드는 로그인/발급 쪽에서만 사용” 같은
 * 식으로 의도를 주석에 박아두면 혼동이 줄어듭니다. <p>
 */
@Getter
@Builder
@RequiredArgsConstructor
public class JwtPrincipal {

    private final UUID id;
    private final String userId;
    private final String nickname;
    private final String[] roles;

    public static JwtPrincipal from(JwtService.Claims claims) {
        return JwtPrincipal.builder()
                           .id(claims.getId())
                           .userId(claims.getUserId())
                           .nickname(claims.getNickname())
                           .roles(claims.getRoles())
                           .build();
    }
}
