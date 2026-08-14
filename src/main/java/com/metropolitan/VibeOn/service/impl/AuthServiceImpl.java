package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.*;
import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.FollowRequestRepository;
import com.metropolitan.VibeOn.repository.RoleRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.security.JwtTokenProvider;
import com.metropolitan.VibeOn.service.AuthService;
import com.metropolitan.VibeOn.service.ImageStorageService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserLookupService userLookupService;
    private final ImageStorageService imageStorageService;

    @Override
    public String login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return jwtTokenProvider.generateToken(authentication);
    }

    @Override
    public void register(MultipartFile image, RegisterDto registerDto) throws IOException {
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email is already taken!");
        }

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image error");
        }

        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setName(registerDto.getName());
        user.setLastName(registerDto.getLastName());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setBirthDate(registerDto.getBirthDate());
        user.setGender(registerDto.isGender());
        user.setCreatedAt(LocalDate.now());

        user.setProfileImageUrl(imageStorageService.save(image));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
    }

    @Override
    public HeaderUserDto getHeaderUserInfo() {
        User currentUser = userLookupService.getCurrentUser();
        return new HeaderUserDto(currentUser.getUsername(), currentUser.getProfileImageUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDto getProfileInfo(String username) {
        User currentUser = userLookupService.getCurrentUser();
        User otherUser = userLookupService.getByUsername(username);

        boolean isFollowing = followRepository
                .existsByFollowerIdAndFolloweeId(currentUser.getId(), otherUser.getId());
        boolean requestPending = followRequestRepository
                .existsByRequesterIdAndTargetId(currentUser.getId(), otherUser.getId());
        boolean canViewPosts = currentUser.getId().equals(otherUser.getId())
                || !otherUser.isPrivate()
                || isFollowing;

        return ProfileDto.fromUser(
                otherUser,
                isFollowing,
                requestPending,
                canViewPosts,
                followRepository.countByFolloweeId(otherUser.getId()),
                followRepository.countByFollowerId(otherUser.getId())
        );
    }

    @Override
    @Transactional
    public ProfileDto updateProfile(MultipartFile image, UpdateProfileDto updateProfileDto) throws IOException {
        User currentUser = userLookupService.getCurrentUser();
        String newProfileImageUrl = null;
        if (image != null && !image.isEmpty()) {
            newProfileImageUrl = imageStorageService.save(image);
        }

        currentUser.setName(updateProfileDto.name().trim());
        currentUser.setLastName(updateProfileDto.lastName().trim());
        currentUser.setBio(updateProfileDto.bio() == null ? null : updateProfileDto.bio().trim());

        if (newProfileImageUrl != null) {
            currentUser.setProfileImageUrl(newProfileImageUrl);
        }

        return getProfileInfo(currentUser.getUsername());
    }
}
