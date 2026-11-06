package com.fitnessbuddy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the database constraints from V2 directly with SQL, bypassing the
 * application. These are the last line of defence: they must hold even if a
 * bug (or a race between two requests) slips past the service-layer checks.
 *
 * {@code @DataJpaTest} starts only the persistence layer and wraps each test
 * in a transaction that is rolled back afterwards, so tests don't see each
 * other's data.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SchemaConstraintsTests {

    @Autowired
    JdbcTemplate jdbc;

    private int userId;

    @BeforeEach
    void insertUser() {
        userId = insertUser("anhad", "anhad@example.com");
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        assertThatThrownBy(() -> insertUser("someone_else", "ANHAD@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_users_email");
    }

    @Test
    void userNameIsUniqueIgnoringCase() {
        assertThatThrownBy(() -> insertUser("ANHAD", "other@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_users_user_name");
    }

    @Test
    void passwordIsRequired() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO users (user_name, email, password, role, created_at)
                VALUES ('nopass', 'nopass@example.com', NULL, 'USER', now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void postBodyAcceptsMoreThan255Characters() {
        insertPost("a".repeat(5000));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM posts", Integer.class)).isEqualTo(1);
    }

    // One violation per test: after any error, Postgres aborts the whole
    // transaction (SQLSTATE 25P02) and rejects every further statement in it.
    @Test
    void postBodyRejectsEmpty() {
        assertThatThrownBy(() -> insertPost(""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_posts_body_length");
    }

    @Test
    void postBodyRejectsMoreThan5000Characters() {
        assertThatThrownBy(() -> insertPost("a".repeat(5001)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_posts_body_length");
    }

    @Test
    void postBodyIsRequired() {
        assertThatThrownBy(() -> insertPost(null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int insertUser(String userName, String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (user_name, email, password, role, created_at)
                VALUES (?, ?, 'hash', 'USER', now()) RETURNING id""",
                Integer.class, userName, email);
    }

    private void insertPost(String body) {
        jdbc.update("INSERT INTO posts (body, user_id, created_at) VALUES (?, ?, now())", body, userId);
    }
}
