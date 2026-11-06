package com.fitnessbuddy.post.dto;

import java.util.List;

public record PostFeedResponse(
        List<ResponsePostDto> posts,
        String nextCursor,
        boolean hasMore
) {}
