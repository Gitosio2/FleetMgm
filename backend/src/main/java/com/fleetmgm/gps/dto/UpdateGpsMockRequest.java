package com.fleetmgm.gps.dto;

import jakarta.validation.constraints.NotNull;

// Boolean, not boolean: a missing field must fail validation with a 400 rather than defaulting to
// false and silently reporting a switch-off the caller never asked for.
public record UpdateGpsMockRequest(@NotNull Boolean enabled) {}
