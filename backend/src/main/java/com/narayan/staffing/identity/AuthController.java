package com.narayan.staffing.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request, HttpServletRequest http) {
        return authService.refresh(request.refreshToken(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    public AuthDtos.MessageResponse logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
        authService.logout(request.refreshToken());
        return new AuthDtos.MessageResponse("Signed out.");
    }

    @GetMapping("/me")
    public AuthDtos.UserProfile me() {
        return authService.me();
    }

    @PostMapping("/forgot-password")
    public AuthDtos.MessageResponse forgot(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        return authService.forgot(request.email());
    }

    @PostMapping("/reset-password")
    public AuthDtos.MessageResponse reset(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        return authService.reset(request.token(), request.password());
    }

    @PostMapping("/change-password")
    public AuthDtos.MessageResponse change(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        return authService.change(request.currentPassword(), request.newPassword());
    }
}
