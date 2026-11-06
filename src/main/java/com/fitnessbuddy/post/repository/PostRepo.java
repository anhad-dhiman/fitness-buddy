package com.fitnessbuddy.post.repository;

import com.fitnessbuddy.post.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepo extends JpaRepository<PostEntity, Integer> {

    @Query("""
        SELECT p FROM PostEntity p
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<PostEntity> findFirstPage(Pageable pageable);

    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.createdAt < :createdAt
           OR (p.createdAt = :createdAt AND p.id < :id)
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<PostEntity> findNextPage(
            @Param("createdAt") Instant createdAt,
            @Param("id") Integer id,
            Pageable pageable
    );
}
