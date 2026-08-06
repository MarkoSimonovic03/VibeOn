package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultDto {
    private Long id;
    private String username;
    private String name;
    private String lastName;
    private String profileImageUrl;

    public static UserSearchResultDto fromUser(User user) {
        return new UserSearchResultDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getLastName(),
                user.getProfileImageUrl()
        );
    }
}
