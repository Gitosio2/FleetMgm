package com.fleetmgm.shared.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
        @Index(name = "idx_audit_performed_at", columnList = "performedAt"),
        @Index(name = "idx_audit_user", columnList = "performedByUserId")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String entityType;

    @Column(length = 100)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String action;

    private UUID performedByUserId;

    @Column(length = 255)
    private String performedByEmail;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 50)
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String newValues;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void prePersist() {
        if (performedAt == null) performedAt = LocalDateTime.now();
    }
}
