package com.fitnessbuddy.auth.service;

import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.common.exception.ConflictException;
import com.fitnessbuddy.common.exception.UnauthorizedException;
import com.fitnessbuddy.common.mapper.Mapper;
import com.fitnessbuddy.security.JwtService;
import com.fitnessbuddy.user.dto.UserResponseDto;
import com.fitnessbuddy.user.entity.UserEntity;
import com.fitnessbuddy.user.repository.UserRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final Mapper<UserEntity, UserResponseDto> userMapper;
    private final JwtService jwtService;

    /**
     * A real BCrypt hash of a throwaway value, computed once at startup. Login
     * checks an unknown email's password against it, so "no such email" costs
     * the same BCrypt work as "wrong password".
     */
    private final String dummyPasswordHash;

    AuthServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder,
                    Mapper<UserEntity, UserResponseDto> userMapper, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equalisation-dummy-password");
    }

    @Override
    @Transactional
    public UserResponseDto registerUser(RegisterRequest registerRequest) {
        String email = normalizeEmail(registerRequest.getEmail());
        String userName = registerRequest.getUserName();

        // Fast path: precise error messages for the common case.
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepo.existsByUserNameIgnoreCase(userName)) {
            throw new ConflictException("This username is already taken");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(userName);
        userEntity.setEmail(email);
        userEntity.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userEntity.setRole("USER");
        userEntity.setCreatedAt(Instant.now());
        userEntity.setAccessedAt(Instant.now());

        // The checks above are "check-then-act": two simultaneous requests can
        // both pass them before either inserts. The unique indexes in the
        // database are what really guarantee one account per email/username.
        // saveAndFlush forces the INSERT now (instead of at commit), so a
        // violation surfaces here, where we can turn it into a clean 409.
        try {
            userRepo.saveAndFlush(userEntity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email or username is already in use");
        }

        return userMapper.mapToDto(userEntity);
    }

    @Override
    @Transactional
    public LoginResult loginUser(LoginRequest loginRequest) {
        Optional<UserEntity> found = userRepo.findByEmailIgnoreCase(normalizeEmail(loginRequest.getEmail()));

        // Always run BCrypt (~100 ms, deliberately slow), even for an unknown
        // email. Otherwise unknown emails would answer measurably faster, and
        // an attacker could use the response time to discover which emails
        // are registered. The error message is identical for the same reason.
        String hashToCheck = found.map(UserEntity::getPassword).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException("Invalid email or password");
        }
        UserEntity user = found.get();

        // No explicit UPDATE needed: inside @Transactional the entity is
        // "managed", and Hibernate's dirty checking writes the change on commit.
        user.setAccessedAt(Instant.now());

        String token = jwtService.generateJwtToken(user);
        return new LoginResult(token, userMapper.mapToDto(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
