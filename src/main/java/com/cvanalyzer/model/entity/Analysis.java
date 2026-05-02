package com.cvanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cv_id", nullable = false)
    private UUID cvId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "score")
    private Integer score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "jsonb")
    private List<String> weaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvements", columnDefinition = "jsonb")
    private List<String> improvements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_keywords", columnDefinition = "jsonb")
    private List<String> missingKeywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections", columnDefinition = "jsonb")
    private Map<String, Object> sections;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        if (this.status == null) this.status = "PROCESSING";
    }
}