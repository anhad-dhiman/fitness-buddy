package com.fitnessBuddy.fitness_buddy.repositories;

import com.fitnessBuddy.fitness_buddy.domain.entities.UserEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepo extends CrudRepository<UserEntity,Integer> {
    Optional<UserEntity> findByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE UserEntity u SET u.accessedAt = :accessedAt WHERE u.id = :id")
    void updateAccessedAt(@Param("accessedAt") Instant accessedAt, @Param("id") Integer id);

}
