package com.fitnessbuddy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway PostgreSQL in Docker for integration tests.
 *
 * {@code @ServiceConnection} makes Spring Boot point the DataSource at this
 * container automatically: no URL/username/password properties needed.
 * Tests therefore run against the same database engine as production, and
 * Flyway migrations are executed for real on every run.
 *
 * Import it with {@code @Import(TestcontainersConfiguration.class)}. Requires a
 * running Docker daemon.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }
}
