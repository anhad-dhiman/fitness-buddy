package com.fitnessbuddy.post.dto;

import java.time.Instant;

public record PostCursor(
        Instant createdAt,
        Integer id
) {}
