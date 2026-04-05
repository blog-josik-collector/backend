package com.backend.userservice.user.repository;

import com.backend.commondb.user.QUser;
import com.backend.commondb.user.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public User findById(UUID userId) {
        QUser user = QUser.user;
        return queryFactory.select(user)
                           .from(user)
                           .where(user.id.eq(userId))
                           .fetchOne();
    }
}
