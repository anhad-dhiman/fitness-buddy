package com.fitnessbuddy.post.repository;

import com.fitnessbuddy.TestcontainersConfiguration;
import com.fitnessbuddy.post.entity.PostEntity;
import com.fitnessbuddy.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the keyset (cursor) pagination queries behind the feed.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PostRepoTests {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    TestEntityManager em;

    @Autowired
    PostRepo postRepo;

    private UserEntity author;

    @BeforeEach
    void setUp() {
        author = em.persist(UserEntity.builder()
                .userName("author").email("author@example.com")
                .password("hash").role("USER").createdAt(T0).build());
    }

    @Test
    void firstPageReturnsNewestPostsFirst() {
        PostEntity oldest = post("oldest", T0);
        PostEntity middle = post("middle", T0.plusSeconds(60));
        PostEntity newest = post("newest", T0.plusSeconds(120));

        List<PostEntity> page = postRepo.findFirstPage(PageRequest.of(0, 2));

        assertThat(page).containsExactly(newest, middle);
    }

    @Test
    void pagingWithTheCursorVisitsEveryPostExactlyOnceEvenWithIdenticalTimestamps() {
        // Several posts share created_at. Paging on created_at alone would skip
        // or repeat them at page boundaries; the id tie-breaker prevents that.
        List<PostEntity> created = List.of(
                post("p1", T0), post("p2", T0), post("p3", T0),
                post("p4", T0.plusSeconds(1)), post("p5", T0.plusSeconds(1)));

        List<Integer> seen = new java.util.ArrayList<>();
        List<PostEntity> page = postRepo.findFirstPage(PageRequest.of(0, 2));
        while (!page.isEmpty()) {
            page.forEach(p -> seen.add(p.getId()));
            PostEntity last = page.getLast();
            page = postRepo.findNextPage(last.getCreatedAt(), last.getId(), PageRequest.of(0, 2));
        }

        assertThat(seen)
                .hasSize(created.size())
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(created.stream().map(PostEntity::getId).toList());
    }

    private PostEntity post(String body, Instant createdAt) {
        PostEntity post = em.persist(PostEntity.builder()
                .body(body).user(author).createdAt(createdAt).build());
        em.flush();
        return post;
    }
}
