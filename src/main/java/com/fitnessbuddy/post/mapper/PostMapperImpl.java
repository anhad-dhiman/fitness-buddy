package com.fitnessbuddy.post.mapper;

import com.fitnessbuddy.post.dto.ResponsePostDto;
import com.fitnessbuddy.post.entity.PostEntity;
import com.fitnessbuddy.common.mapper.Mapper;
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
