package com.fitnessbuddy.post.dto;

import com.fitnessbuddy.user.dto.UserResponseDto;
import com.fitnessbuddy.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponsePostDto {
    private Integer id;
    private String body;
    private UserResponseDto user;
    private Instant createdAt;
}
