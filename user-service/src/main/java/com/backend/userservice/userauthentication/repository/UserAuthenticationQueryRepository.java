package com.backend.userservice.userauthentication.repository;

import com.backend.commondataaccess.persistence.user.QUser;
import com.backend.commondataaccess.persistence.user.QUserAuthentication;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserAuthenticationQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QUserAuthentication userAuthentication = QUserAuthentication.userAuthentication;
    private final QUser user = QUser.user;

    public Optional<UserAuthentication> findOneById(UUID id) {
        return Optional.ofNullable(queryFactory
                                           .selectFrom(userAuthentication)
                                           .join(userAuthentication.user, user).fetchJoin()
                                           .where(
                                                   userAuthentication.id.eq(id),
                                                   userAuthentication.deletedAt.isNull()
                                           )
                                           .fetchOne());
    }

    public Optional<UserAuthentication> findOneByIdentifier(String identifier) {
        return Optional.ofNullable(queryFactory
                                           .selectFrom(userAuthentication)
                                           .join(userAuthentication.user, user).fetchJoin()
                                           .where(
                                                   userAuthentication.identifier.eq(identifier),
                                                   userAuthentication.deletedAt.isNull()
                                           )
                                           .fetchOne());
    }

    public List<UserAuthentication> findAllByUserId(UUID userId) {
        return queryFactory
                .selectFrom(userAuthentication)
                .join(userAuthentication.user, user).fetchJoin()
                .where(
                        userAuthentication.user.id.eq(userId),
                        userAuthentication.deletedAt.isNull()
                )
                .fetch();
    }

}
