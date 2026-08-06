package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.CreateReportDto;
import com.metropolitan.VibeOn.dto.ReportResponseDto;
import com.metropolitan.VibeOn.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(@Valid @RequestBody CreateReportDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(request));
    }
}
