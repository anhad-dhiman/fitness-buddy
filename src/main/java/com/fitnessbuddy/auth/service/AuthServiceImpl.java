package com.fitnessbuddy.auth.service;

import com.fitnessbuddy.user.dto.UserResponseDto;
import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.user.entity.UserEntity;
import com.fitnessbuddy.common.mapper.Mapper;
import com.fitnessbuddy.user.repository.UserRepo;
import com.fitnessbuddy.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    UserRepo userRepo;
    PasswordEncoder passwordEncoder;
    private final Mapper<UserEntity, UserResponseDto> userMapper;
    JwtService jwtService;

    AuthServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder,
                    Mapper<UserEntity, UserResponseDto> userMapper, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }


    @Override
    public UserResponseDto registerUser(RegisterRequest registerRequest) {
        UserEntity userEntity = new UserEntity();

        userEntity.setUserName(registerRequest.getUserName());
        userEntity.setEmail(registerRequest.getEmail());

        String hashedPass =  passwordEncoder.encode(registerRequest.getPassword());
        userEntity.setPassword(hashedPass);

        userEntity.setRole("USER");

        userEntity.setCreatedAt(Instant.now());
        userEntity.setAccessedAt(Instant.now());

        userRepo.save(userEntity);

        return userMapper.mapToDto(userEntity);
    }

    @Override
    public ResponseEntity<UserResponseDto> loginUser(LoginRequest loginRequest,
                                                     HttpServletResponse response) {
        String email = loginRequest.getEmail();
        String rawPassword = loginRequest.getPassword();

        Optional<UserEntity> userEntity = userRepo.findByEmail(email);

        if(userEntity.isPresent()){
            UserEntity user = userEntity.get();
            String hashedPassword = user.getPassword();

            if(passwordEncoder.matches(rawPassword, hashedPassword)){
                user.setAccessedAt(Instant.now());
                userRepo.updateAccessedAt(user.getAccessedAt(), user.getId());

                String token = jwtService.generateJwtToken(user);
                ResponseCookie cookie = ResponseCookie
                        .from("accessToken", token)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build();

                response.addHeader(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                );

                return ResponseEntity.ok(userMapper.mapToDto(user));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
