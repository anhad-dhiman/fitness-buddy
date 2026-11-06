package com.fitnessBuddy.fitness_buddy.domain.dto.posts;

import java.time.Instant;

public record PostCursor(
        Instant createdAt,
        Integer id
) {}
