package com.fitnessBuddy.fitness_buddy.mappers.impl;

import com.fitnessBuddy.fitness_buddy.domain.dto.posts.ResponsePostDto;
import com.fitnessBuddy.fitness_buddy.domain.entities.PostEntity;
import com.fitnessBuddy.fitness_buddy.mappers.Mapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class PostMapperImpl implements Mapper<PostEntity, ResponsePostDto> {

    private final ModelMapper modelMapper;

    public PostMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponsePostDto mapToDto(PostEntity postEntity) {
        return modelMapper.map(postEntity, ResponsePostDto.class);
    }

    @Override
    public PostEntity mapFromDto(ResponsePostDto  responsePostDto) {
        return modelMapper.map(responsePostDto, PostEntity.class);
    }
}
