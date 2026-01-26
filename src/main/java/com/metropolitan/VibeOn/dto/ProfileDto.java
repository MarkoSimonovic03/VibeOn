package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private Long id;
    private String username;
    private String name;
    private String lastName;
    private LocalDate createdAt;
    private String email;
    private String profileImageUrl;
    private LocalDate birthDate;
    private boolean gender;
    private Boolean isFollowing;

    public static ProfileDto fromUser(User user, Boolean isFollowing) {
        return new ProfileDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getBirthDate(),
                user.isGender(),
                isFollowing
        );
    }
}
