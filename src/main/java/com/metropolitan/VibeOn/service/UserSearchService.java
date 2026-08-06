package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.UserSearchResultDto;

import java.util.List;

public interface UserSearchService {
    List<UserSearchResultDto> searchUsers(String query);
}
