package com.fitnessbuddy.user.repository;

import com.fitnessbuddy.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The lookups compare lower(column) = lower(:value) on purpose. The unique
 * indexes in V2 are on lower(email) / lower(user_name), and PostgreSQL only
 * uses an expression index when the query uses the same expression. A plain
 * "WHERE email = ?" would scan the whole table. (Spring Data's derived
 * "...IgnoreCase" methods generate upper(...), which would also miss the index.)
 */
@Repository
public interface UserRepo extends JpaRepository<UserEntity, Integer> {

    @Query("SELECT u FROM UserEntity u WHERE lower(u.email) = lower(:email)")
    Optional<UserEntity> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT count(u) > 0 FROM UserEntity u WHERE lower(u.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT count(u) > 0 FROM UserEntity u WHERE lower(u.userName) = lower(:userName)")
    boolean existsByUserNameIgnoreCase(@Param("userName") String userName);
}
