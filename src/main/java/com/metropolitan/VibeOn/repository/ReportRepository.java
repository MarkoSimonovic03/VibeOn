package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndActiveTrue(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    );
}
