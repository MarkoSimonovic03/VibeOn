package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.LoginDto;
import com.metropolitan.VibeOn.dto.RegisterDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AuthService {
    String login(LoginDto loginDto);

    void register(MultipartFile image, RegisterDto registerDto) throws IOException;
}
