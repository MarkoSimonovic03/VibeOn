package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.CreateReportDto;
import com.metropolitan.VibeOn.dto.ReportResponseDto;

public interface ReportService {
    ReportResponseDto createReport(CreateReportDto request);
}
