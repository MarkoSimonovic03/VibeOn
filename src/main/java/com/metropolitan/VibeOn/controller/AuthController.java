package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.JWTAuthResponse;
import com.metropolitan.VibeOn.dto.LoginDto;
import com.metropolitan.VibeOn.dto.RegisterDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<JWTAuthResponse> authenticate(@RequestBody LoginDto loginDto) {
        String token = authService.login(loginDto);

        JWTAuthResponse jwtAuthResponse = new JWTAuthResponse();
        jwtAuthResponse.setAccessToken(token);

        return ResponseEntity.ok(jwtAuthResponse);
    }

        @PostMapping("/register")
        public ResponseEntity<String> register(@RequestPart("image") MultipartFile image, @RequestPart("registerDto") RegisterDto registerDto) {
            try {
                String response = authService.register(image, registerDto);
                return ResponseEntity.ok(response);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(400).body("Invalid data: " + e.getMessage());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
            }
        }
}
