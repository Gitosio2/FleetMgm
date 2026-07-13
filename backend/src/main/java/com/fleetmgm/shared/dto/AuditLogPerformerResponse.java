package com.fleetmgm.shared.dto;

// Minimal projection for the audit log's user filter dropdown — scoped intentionally to the
// distinct performers already present in audit_logs, not the full users table (no "Gestión de
// usuarios y roles" feature exists yet, see planning.md). email doubles as both the filter value
// and the display label since User has no separate display name.
public record AuditLogPerformerResponse(String email) {}
