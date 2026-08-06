package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLookupService {
    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getByUsername(authentication.getName());
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));
    }
}
