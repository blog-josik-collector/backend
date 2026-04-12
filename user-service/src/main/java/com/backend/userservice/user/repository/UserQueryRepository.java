package com.backend.userservice.user.repository;

import com.backend.commondataaccess.persistence.user.QUser;
import com.backend.commondataaccess.persistence.user.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    QUser user = QUser.user;

    public Optional<User> findOneById(UUID id) {
        User result = queryFactory.select(user)
                                  .from(user)
                                  .where(user.id.eq(id))
                                  .where(user.deletedAt.isNull())
                                  .fetchOne();

        return Optional.ofNullable(result);
    }
}
