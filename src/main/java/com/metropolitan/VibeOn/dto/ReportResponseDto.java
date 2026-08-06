package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.ReportReason;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReportResponseDto {
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String description;
    private LocalDateTime createdAt;

    public static ReportResponseDto fromReport(Report report) {
        return new ReportResponseDto(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDescription(),
                report.getCreatedAt()
        );
    }
}
