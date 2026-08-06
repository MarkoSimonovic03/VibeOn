package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.UserSearchResultDto;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.UserLookupService;
import com.metropolitan.VibeOn.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl implements UserSearchService {
    private static final int MAX_RESULTS = 20;

    private final UserRepository userRepository;
    private final UserLookupService userLookupService;

    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchUsers(String query) {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return userRepository.searchUsers(
                        query.trim(),
                        currentUserId,
                        PageRequest.of(0, MAX_RESULTS)
                ).stream()
                .map(UserSearchResultDto::fromUser)
                .toList();
    }
}
