package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.ModerationRemovalRequest;
import com.metropolitan.VibeOn.dto.ModerationRemovalResponseDto;
import com.metropolitan.VibeOn.entity.ModerationTargetType;
import com.metropolitan.VibeOn.dto.FollowModerationRequest;
import com.metropolitan.VibeOn.dto.FollowModerationResponseDto;
import com.metropolitan.VibeOn.dto.UserSuspensionRequest;
import com.metropolitan.VibeOn.dto.UserSuspensionResponseDto;
import com.metropolitan.VibeOn.dto.ReportDismissalRequest;
import com.metropolitan.VibeOn.dto.ReportDismissalResponseDto;
import com.metropolitan.VibeOn.dto.ContentRestorationRequest;
import com.metropolitan.VibeOn.dto.ContentRestorationResponseDto;
import com.metropolitan.VibeOn.dto.MentionModerationResponseDto;

import java.util.List;
import com.metropolitan.VibeOn.dto.ModerationHistoryDto;

public interface ModerationService {
    ModerationRemovalResponseDto removeContent(
            ModerationTargetType targetType,
            Long targetId,
            ModerationRemovalRequest request
    );

    UserSuspensionResponseDto suspendUser(Long userId, UserSuspensionRequest request);

    FollowModerationResponseDto removeFollow(Long followerId, Long followeeId, FollowModerationRequest request);

    ReportDismissalResponseDto dismissReport(Long reportId, ReportDismissalRequest request);

    ContentRestorationResponseDto restoreContent(Long actionId, ContentRestorationRequest request);

    MentionModerationResponseDto removeMentions(
            ModerationTargetType targetType,
            Long targetId,
            ModerationRemovalRequest request
    );

    List<ModerationHistoryDto> getHistory();
}
