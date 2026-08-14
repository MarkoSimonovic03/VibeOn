package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.JWTAuthResponse;
import com.metropolitan.VibeOn.dto.LoginDto;
import com.metropolitan.VibeOn.dto.RegisterDto;
import com.metropolitan.VibeOn.dto.ProfileDto;
import com.metropolitan.VibeOn.dto.UpdateProfileDto;
import com.metropolitan.VibeOn.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JWTAuthResponse> authenticate(@Valid @RequestBody LoginDto loginDto) {
        JWTAuthResponse jwtAuthResponse = new JWTAuthResponse();
        jwtAuthResponse.setAccessToken(authService.login(loginDto));

        return ResponseEntity.ok(jwtAuthResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestPart("image") MultipartFile image,
            @Valid @RequestPart("registerDto") RegisterDto registerDto
    ) {
        try {
            authService.register(image, registerDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid registration data.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/header-info")
    public ResponseEntity<?> headerInfo() {
        try {
            return ResponseEntity.ok(authService.getHeaderUserInfo());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getProfileInfo(@PathVariable String username) {
        try {
            return ResponseEntity.ok(authService.getProfileInfo(username));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileDto> updateProfile(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @Valid @RequestPart("profile") UpdateProfileDto updateProfileDto
    ) throws IOException {
        return ResponseEntity.ok(authService.updateProfile(image, updateProfileDto));
    }
}
