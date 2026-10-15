package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.CreateReportDto;
import com.metropolitan.VibeOn.dto.ModeratorReportDto;
import com.metropolitan.VibeOn.dto.ReportResponseDto;
import com.metropolitan.VibeOn.entity.ReportTargetType;

import java.util.List;

public interface ReportService {
    ReportResponseDto createReport(CreateReportDto request);
    List<ModeratorReportDto> getActiveReports(ReportTargetType targetType);
    ModeratorReportDto getActiveReport(Long reportId);
}
