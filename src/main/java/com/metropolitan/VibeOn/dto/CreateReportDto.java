package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.ReportReason;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportDto {
    @NotNull
    private ReportTargetType targetType;

    @NotNull
    @Positive
    private Long targetId;

    @NotNull
    private ReportReason reason;

    @Size(max = 1000)
    private String description;
}
