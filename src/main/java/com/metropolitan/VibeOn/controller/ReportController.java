package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.CreateReportDto;
import com.metropolitan.VibeOn.dto.ModeratorReportDto;
import com.metropolitan.VibeOn.dto.ReportResponseDto;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import com.metropolitan.VibeOn.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(@Valid @RequestBody CreateReportDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(request));
    }

    @GetMapping("/moderation")
    public ResponseEntity<List<ModeratorReportDto>> getActiveReports(
            @RequestParam(required = false) ReportTargetType targetType
    ) {
        return ResponseEntity.ok(reportService.getActiveReports(targetType));
    }

    @GetMapping("/moderation/{reportId}")
    public ResponseEntity<ModeratorReportDto> getActiveReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.getActiveReport(reportId));
    }
}
