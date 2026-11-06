package com.fitnessBuddy.fitness_buddy.domain.dto.posts;

import java.util.List;

public record PostFeedResponse(
        List<ResponsePostDto> posts,
        String nextCursor,
        boolean hasMore
) {}
