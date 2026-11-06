package com.fitnessbuddy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Boots the whole application against a real (containerised) PostgreSQL.
 * Passing proves that all Flyway migrations apply to an empty database and
 * that Hibernate's `ddl-auto: validate` accepts the resulting schema.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FitnessBuddyApplicationTests {

	@Test
	void contextLoads() {
	}

}
