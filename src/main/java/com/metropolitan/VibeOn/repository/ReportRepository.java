package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndActiveTrue(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    );

    @EntityGraph(attributePaths = "reporter")
    List<Report> findAllByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "reporter")
    List<Report> findAllByActiveTrueAndTargetTypeOrderByCreatedAtDesc(ReportTargetType targetType);

    @EntityGraph(attributePaths = "reporter")
    Optional<Report> findByIdAndActiveTrue(Long id);

    List<Report> findAllByTargetTypeAndTargetIdAndActiveTrue(ReportTargetType targetType, Long targetId);
}
