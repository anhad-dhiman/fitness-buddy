package com.fitnessbuddy.user.mapper;

import com.fitnessbuddy.user.dto.UserResponseDto;
import com.fitnessbuddy.user.entity.UserEntity;
import com.fitnessbuddy.common.mapper.Mapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements Mapper<UserEntity, UserResponseDto> {

    private final ModelMapper modelMapper;

    public UserMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public UserResponseDto mapToDto(UserEntity userEntity) {
        return modelMapper.map(userEntity, UserResponseDto.class);
    }

    @Override
    public UserEntity mapFromDto(UserResponseDto userResponseDto) {
        return modelMapper.map(userResponseDto, UserEntity.class);
    }

}