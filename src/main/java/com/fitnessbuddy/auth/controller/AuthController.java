package com.fitnessbuddy.auth.controller;

import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.auth.service.AuthService;
import com.fitnessbuddy.auth.service.LoginResult;
import com.fitnessbuddy.security.AccessTokenCookieFactory;
import com.fitnessbuddy.user.dto.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessTokenCookieFactory accessTokenCookieFactory;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(AuthService authService, AccessTokenCookieFactory accessTokenCookieFactory,
                          CsrfTokenRepository csrfTokenRepository) {
        this.authService = authService;
        this.accessTokenCookieFactory = accessTokenCookieFactory;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * Gives the frontend its CSRF token. Call it once when the app loads, before
     * the first POST (register and login are protected too). The response sets
     * the XSRF-TOKEN cookie; JavaScript reads that cookie and sends its value in
     * the X-XSRF-TOKEN header (axios does this automatically).
     *
     * Spring generates CSRF tokens lazily; asking for the token here is what
     * makes it write the cookie.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    // @Valid runs the Bean Validation annotations on the DTO before this method
    // is entered; failures become a 400 in GlobalExceptionHandler.
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequest loginRequest,
                                                 HttpServletRequest request, HttpServletResponse response) {
        LoginResult result = authService.loginUser(loginRequest);
        // Both cookies are added straight to the servlet response. A Set-Cookie
        // header on the returned ResponseEntity would REPLACE every Set-Cookie
        // already written (here: the new XSRF-TOKEN), not add to them.
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.create(result.accessToken()).toString());
        rotateCsrfToken(request, response);
        return ResponseEntity.ok(result.user());
    }

    /**
     * Deletes the auth cookie in the browser. Public, so it also works with an
     * already-expired token.
     *
     * Limitation (fixed by refresh tokens in Milestone 1.8): a JWT is valid
     * until it expires; the server keeps no list of issued tokens. If a token
     * was copied before logout, it still works for up to 15 minutes.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.clear().toString());
        rotateCsrfToken(request, response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Issues a new CSRF token (a new XSRF-TOKEN cookie in this response) whenever
     * the user's authentication changes. A token an attacker managed to plant in
     * the browser before login is therefore useless afterwards. The frontend
     * just keeps reading the cookie before each request.
     */
    private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
    }
}
